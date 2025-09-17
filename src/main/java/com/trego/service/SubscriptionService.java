package com.trego.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import com.stripe.net.Webhook;
import com.trego.model.User;
import com.trego.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class SubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);
    
    // Stripe Price IDs for different subscription tiers
    private static final String PREMIUM_MONTHLY_PRICE_ID = "price_premium_monthly";
    private static final String PREMIUM_YEARLY_PRICE_ID = "price_premium_yearly";
    private static final String PREMIUM_PLUS_MONTHLY_PRICE_ID = "price_premium_plus_monthly";
    private static final String PREMIUM_PLUS_YEARLY_PRICE_ID = "price_premium_plus_yearly";
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    public void initializeStripe() {
        Stripe.apiKey = stripeSecretKey;
    }
    
    public Customer createCustomer(String email, String name, String userId) throws StripeException {
        logger.info("Creating Stripe customer for user: {}", userId);
        
        initializeStripe();
        
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .putMetadata("userId", userId)
                .putMetadata("source", "trego-app")
                .build();
        
        Customer customer = Customer.create(params);
        logger.info("Stripe customer created with ID: {}", customer.getId());
        
        return customer;
    }
    
    public PaymentIntent createPaymentIntent(String userId, String subscriptionTier, String billingPeriod) 
            throws StripeException, ExecutionException, InterruptedException {
        
        logger.info("Creating payment intent for user: {} - {} {}", userId, subscriptionTier, billingPeriod);
        
        initializeStripe();
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        long amount = calculateSubscriptionAmount(subscriptionTier, billingPeriod);
        
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency("usd")
                .setCustomer(getOrCreateCustomerId(user))
                .putMetadata("userId", userId)
                .putMetadata("subscriptionTier", subscriptionTier)
                .putMetadata("billingPeriod", billingPeriod)
                .setDescription(String.format("Trego %s Subscription (%s)", subscriptionTier, billingPeriod))
                .build();
        
        PaymentIntent intent = PaymentIntent.create(params);
        logger.info("Payment intent created: {}", intent.getId());
        
        return intent;
    }
    
    public Subscription createSubscription(String userId, String priceId, String paymentMethodId) 
            throws StripeException, ExecutionException, InterruptedException {
        
        logger.info("Creating subscription for user: {} with price: {}", userId, priceId);
        
        initializeStripe();
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        String customerId = getOrCreateCustomerId(user);
        
        // Attach payment method to customer
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
        paymentMethod.attach(PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build());
        
        // Create subscription
        SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
                .addExpand("latest_invoice.payment_intent")
                .putMetadata("userId", userId)
                .build();
        
        Subscription subscription = Subscription.create(params);
        
        // Update user subscription status
        updateUserSubscriptionStatus(user, subscription);
        
        logger.info("Subscription created successfully: {}", subscription.getId());
        return subscription;
    }
    
    public void cancelSubscription(String userId) throws StripeException, ExecutionException, InterruptedException {
        logger.info("Cancelling subscription for user: {}", userId);
        
        initializeStripe();
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        if (user.getSubscriptionId() == null) {
            throw new IllegalArgumentException("No active subscription found");
        }
        
        Subscription subscription = Subscription.retrieve(user.getSubscriptionId());
        subscription.cancel();
        
        // Update user to free tier
        user.setSubscriptionStatus("FREE");
        user.setSubscriptionId(null);
        userRepository.update(user);
        
        logger.info("Subscription cancelled for user: {}", userId);
    }
    
    public void handleWebhook(String payload, String sigHeader) throws StripeException {
        logger.info("Processing Stripe webhook");
        
        initializeStripe();
        
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeSecretKey);
            
            switch (event.getType()) {
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionCancelled(event);
                    break;
                case "invoice.payment_succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handlePaymentFailed(event);
                    break;
                default:
                    logger.info("Unhandled webhook event type: {}", event.getType());
            }
            
        } catch (Exception e) {
            logger.error("Webhook processing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }
    
    public Map<String, Object> getSubscriptionStatus(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting subscription status for user: {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        User user = userOpt.get();
        Map<String, Object> status = new HashMap<>();
        
        status.put("subscriptionStatus", user.getSubscriptionStatus());
        status.put("subscriptionId", user.getSubscriptionId());
        status.put("isPremium", user.isPremiumUser());
        status.put("isTrialActive", user.isTrialActive());
        status.put("trialEndDate", user.getTrialEndDate());
        
        if (user.getSubscriptionId() != null) {
            try {
                initializeStripe();
                Subscription subscription = Subscription.retrieve(user.getSubscriptionId());
                status.put("currentPeriodEnd", subscription.getCurrentPeriodEnd());
                status.put("cancelAtPeriodEnd", subscription.getCancelAtPeriodEnd());
                status.put("stripeStatus", subscription.getStatus());
            } catch (StripeException e) {
                logger.warn("Failed to retrieve Stripe subscription for user {}: {}", userId, e.getMessage());
            }
        }
        
        return status;
    }
    
    private String getOrCreateCustomerId(User user) throws StripeException {
        // Check if user already has a Stripe customer ID (you might store this in user metadata)
        // For now, create a new customer each time
        Customer customer = createCustomer(user.getEmail(), user.getFullName(), user.getId());
        return customer.getId();
    }
    
    private long calculateSubscriptionAmount(String subscriptionTier, String billingPeriod) {
        // Amounts in cents
        return switch (subscriptionTier.toUpperCase()) {
            case "PREMIUM" -> "monthly".equals(billingPeriod) ? 999 : 9999; // $9.99/month or $99.99/year
            case "PREMIUM_PLUS" -> "monthly".equals(billingPeriod) ? 1999 : 19999; // $19.99/month or $199.99/year
            default -> throw new IllegalArgumentException("Invalid subscription tier");
        };
    }
    
    private void updateUserSubscriptionStatus(User user, Subscription subscription) throws ExecutionException, InterruptedException {
        String priceId = subscription.getItems().getData().get(0).getPrice().getId();
        
        String subscriptionTier = switch (priceId) {
            case PREMIUM_MONTHLY_PRICE_ID, PREMIUM_YEARLY_PRICE_ID -> "PREMIUM";
            case PREMIUM_PLUS_MONTHLY_PRICE_ID, PREMIUM_PLUS_YEARLY_PRICE_ID -> "PREMIUM_PLUS";
            default -> "PREMIUM"; // Default fallback
        };
        
        user.setSubscriptionStatus(subscriptionTier);
        user.setSubscriptionId(subscription.getId());
        user.setTrialEndDate(null); // Clear trial when subscription starts
        userRepository.update(user);
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            logger.warn("Failed to send subscription welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    private void handleSubscriptionCreated(Event event) {
        logger.info("Handling subscription created webhook");
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
            if (subscription != null && subscription.getMetadata().containsKey("userId")) {
                String userId = subscription.getMetadata().get("userId");
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    updateUserSubscriptionStatus(userOpt.get(), subscription);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to handle subscription created webhook: {}", e.getMessage(), e);
        }
    }
    
    private void handleSubscriptionUpdated(Event event) {
        logger.info("Handling subscription updated webhook");
        // Handle subscription changes, plan upgrades/downgrades
    }
    
    private void handleSubscriptionCancelled(Event event) {
        logger.info("Handling subscription cancelled webhook");
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
            if (subscription != null && subscription.getMetadata().containsKey("userId")) {
                String userId = subscription.getMetadata().get("userId");
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setSubscriptionStatus("FREE");
                    user.setSubscriptionId(null);
                    userRepository.update(user);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to handle subscription cancelled webhook: {}", e.getMessage(), e);
        }
    }
    
    private void handlePaymentSucceeded(Event event) {
        logger.info("Handling payment succeeded webhook");
        // Handle successful payment, extend subscription period if needed
    }
    
    private void handlePaymentFailed(Event event) {
        logger.info("Handling payment failed webhook");
        // Handle failed payment, notify user, potentially suspend account
    }
}