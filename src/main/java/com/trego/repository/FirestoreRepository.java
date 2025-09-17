package com.trego.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.trego.model.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class FirestoreRepository<T extends BaseEntity> {
    
    private static final Logger logger = LoggerFactory.getLogger(FirestoreRepository.class);
    
    @Autowired
    protected Firestore firestore;
    
    protected final String collectionName;
    protected final Function<Map<String, Object>, T> mapper;
    
    public FirestoreRepository(String collectionName, Function<Map<String, Object>, T> mapper) {
        this.collectionName = collectionName;
        this.mapper = mapper;
    }
    
    public T save(T entity) throws ExecutionException, InterruptedException {
        logger.debug("Saving entity to collection: {}", collectionName);
        
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        entity.updateTimestamp();
        
        Map<String, Object> data = entity.toFirestoreMap();
        
        ApiFuture<WriteResult> future = firestore.collection(collectionName)
                .document(entity.getId())
                .set(data);
        
        WriteResult result = future.get();
        logger.debug("Entity saved successfully at: {}", result.getUpdateTime());
        
        return entity;
    }
    
    public Optional<T> findById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Finding entity by id: {} in collection: {}", id, collectionName);
        
        DocumentReference docRef = firestore.collection(collectionName).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        if (document.exists()) {
            Map<String, Object> data = document.getData();
            if (data != null) {
                T entity = mapper.apply(data);
                logger.debug("Entity found: {}", id);
                return Optional.of(entity);
            }
        }
        
        logger.debug("Entity not found: {}", id);
        return Optional.empty();
    }
    
    public List<T> findAll() throws ExecutionException, InterruptedException {
        logger.debug("Finding all entities in collection: {}", collectionName);
        
        ApiFuture<QuerySnapshot> future = firestore.collection(collectionName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        List<T> entities = documents.stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    return mapper.apply(data);
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} entities in collection: {}", entities.size(), collectionName);
        return entities;
    }
    
    public List<T> findByField(String field, Object value) throws ExecutionException, InterruptedException {
        logger.debug("Finding entities by field: {} = {} in collection: {}", field, value, collectionName);
        
        Query query = firestore.collection(collectionName).whereEqualTo(field, value);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        List<T> entities = documents.stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    return mapper.apply(data);
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} entities by field: {} = {}", entities.size(), field, value);
        return entities;
    }
    
    public List<T> findByFieldIn(String field, List<?> values) throws ExecutionException, InterruptedException {
        logger.debug("Finding entities by field: {} in {} values", field, values.size());
        
        Query query = firestore.collection(collectionName).whereIn(field, values);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        List<T> entities = documents.stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    return mapper.apply(data);
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} entities by field: {} in values", entities.size(), field);
        return entities;
    }
    
    public List<T> findWithPagination(int limit, String lastDocumentId) throws ExecutionException, InterruptedException {
        logger.debug("Finding entities with pagination: limit={}, lastDocumentId={}", limit, lastDocumentId);
        
        Query query = firestore.collection(collectionName).orderBy("createdAt").limit(limit);
        
        if (lastDocumentId != null && !lastDocumentId.isEmpty()) {
            DocumentReference lastDocRef = firestore.collection(collectionName).document(lastDocumentId);
            ApiFuture<DocumentSnapshot> lastDocFuture = lastDocRef.get();
            DocumentSnapshot lastDoc = lastDocFuture.get();
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc);
            }
        }
        
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        List<T> entities = documents.stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    return mapper.apply(data);
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} entities with pagination", entities.size());
        return entities;
    }
    
    public T update(T entity) throws ExecutionException, InterruptedException {
        logger.debug("Updating entity: {} in collection: {}", entity.getId(), collectionName);
        
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Entity ID cannot be null for update operation");
        }
        
        entity.updateTimestamp();
        Map<String, Object> data = entity.toFirestoreMap();
        
        ApiFuture<WriteResult> future = firestore.collection(collectionName)
                .document(entity.getId())
                .set(data);
        
        WriteResult result = future.get();
        logger.debug("Entity updated successfully at: {}", result.getUpdateTime());
        
        return entity;
    }
    
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Deleting entity by id: {} from collection: {}", id, collectionName);
        
        ApiFuture<WriteResult> future = firestore.collection(collectionName).document(id).delete();
        WriteResult result = future.get();
        
        logger.debug("Entity deleted successfully at: {}", result.getUpdateTime());
    }
    
    public boolean existsById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Checking if entity exists: {} in collection: {}", id, collectionName);
        
        DocumentReference docRef = firestore.collection(collectionName).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        boolean exists = document.exists();
        logger.debug("Entity exists: {}", exists);
        
        return exists;
    }
    
    public long count() throws ExecutionException, InterruptedException {
        logger.debug("Counting entities in collection: {}", collectionName);
        
        ApiFuture<QuerySnapshot> future = firestore.collection(collectionName).get();
        QuerySnapshot snapshot = future.get();
        
        long count = snapshot.size();
        logger.debug("Total entities count: {}", count);
        
        return count;
    }
    
    public List<T> findByQuery(Query query) throws ExecutionException, InterruptedException {
        logger.debug("Executing custom query on collection: {}", collectionName);
        
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        List<T> entities = documents.stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    return mapper.apply(data);
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} entities from custom query", entities.size());
        return entities;
    }
    
    public void batchSave(List<T> entities) throws ExecutionException, InterruptedException {
        logger.debug("Batch saving {} entities to collection: {}", entities.size(), collectionName);
        
        WriteBatch batch = firestore.batch();
        
        for (T entity : entities) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
            }
            entity.updateTimestamp();
            
            DocumentReference docRef = firestore.collection(collectionName).document(entity.getId());
            batch.set(docRef, entity.toFirestoreMap());
        }
        
        ApiFuture<List<WriteResult>> future = batch.commit();
        List<WriteResult> results = future.get();
        
        logger.debug("Batch save completed: {} entities saved", results.size());
    }
}