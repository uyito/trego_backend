package com.trego.repository;

import com.trego.model.PostReport;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository extends FirestoreRepository<PostReport> {
    public ReportRepository() {
        super("social_reports", PostReport::fromFirestoreMap);
    }
}
