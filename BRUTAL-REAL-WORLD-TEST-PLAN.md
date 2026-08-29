# PHARMACY POS — BRUTAL REAL-WORLD TEST PLAN

## Mission Statement

This test suite validates whether the Pharmacy POS system can survive **real pharmacy operations under stress, human mistakes, concurrency, failures, restarts, network loss, payment uncertainty, hardware failures, authorization attacks, and recovery** without corrupting business state.

## Architecture Overview

- **Test Runner**: PowerShell scripts executed against a live Docker Compose stack
- **Shared Library**: `brutal-common.ps1` — authentication, API calls, assertions, reconciliation, DB queries
- **Infrastructure**: Docker Compose (PostgreSQL 17.7, Spring Boot API, Next.js frontend)
- **Concurrency**: PowerShell runspaces for parallel request execution
- **Crash Injection**: `docker stop --time=0` for immediate container termination
- **Database Verification**: Direct `psql` queries via `docker exec` for independent reconciliation

## Test Data Model

### Branches
- Pharmacy A → Main Branch (primary test target)
- Pharmacy B → Main Branch (tenant isolation verification)

### Users
| Role | Email | Password | Purpose |
|------|-------|----------|---------|
| Owner | admin@demo.com | admin123 | Full access, reconciliation |
| Manager | manager@demo.com | manager123 | Most operations except user mgmt |
| Pharmacist | pharmacist@demo.com | pharmacist123 | Prescriptions, dispensary |
| Cashier 1 | cashier@demo.com | cashier123 | Sales, shifts |
| Cashier 2 | storekeeper@demo.com | stock1234 | Concurrent sales testing |
| Store Keeper | technician@demo.com | tech12345 | Procurement, inventory |

### Medicines
| Medicine | Type | Stock | Notes |
|----------|------|-------|-------|
| Paracetamol 500mg | OTC | 100 | Standard test medicine |
| Amoxicillin 250mg | Prescription | 50 | Requires prescription |
| Coartem | Controlled | 20 | Controlled substance |
| Near-Expiry Batch | OTC | 10 | Expires in 3 days |
| Zero-Stock Medicine | OTC | 0 | For oversell testing |
| Multi-Batch Medicine | OTC | 50+50 | Two batches, different expiry |

## Test Scenarios

### Group 1: CONCURRENCY (Critical)
1. **C1**: Two terminals sell the last unit simultaneously
2. **C2**: Double-click cashiers (duplicate requests with same idempotency key)
3. **C3**: Concurrent returns on the same sale
4. **C4**: Concurrent shift open attempts for the same user
5. **C5**: Sale during shift auto-close

### Group 2: CRASH RECOVERY (Critical)
6. **R1**: Crash before transaction commit (idempotency key stuck IN_PROGRESS)
7. **R2**: Crash after stock deduction but before sale save
8. **R3**: Restart during payment processing
9. **R4**: Database restart during checkout
10. **R5**: Full stack restart mid-operation

### Group 3: PAYMENT INTEGRITY (Critical)
11. **P1**: M-Pesa manual reference deduplication
12. **P2**: M-Pesa manual reference across branches
13. **P3**: Cash drawer reconciliation after complex operations
14. **P4**: Payment amount mismatch detection
15. **P5**: Credit sale with insufficient credit limit

### Group 4: INVENTORY INTEGRITY (High)
16. **I1**: FEFO verification (near-expiry batch consumed first)
17. **I2**: Multi-batch allocation math verification
18. **I3**: Unit conversion accuracy (Box → Strip → Tablet)
19. **I4**: Stock movement journal completeness
20. **I5**: Return quarantine (stock not directly available)
21. **I6**: Negative stock prevention via CHECK constraint

### Group 5: SHIFT LIFECYCLE (High)
22. **S1**: Full shift lifecycle with complex transactions
23. **S2**: Shift closure cash reconciliation
24. **S3**: Z-report accuracy
25. **S4**: Variance detection and reporting
26. **S5**: Auto-close shift at configured hour

### Group 6: AUTHORIZATION (High)
27. **A1**: Cross-tenant data isolation
28. **A2**: Role-based endpoint access (comprehensive)
29. **A3**: Session management (eviction, timeout)
30. **A4**: CSRF protection verification
31. **A5**: Rate limiting behavior

### Group 7: RETURNS (Medium)
32. **T1**: Multi-item return with different payment methods
33. **T2**: Over-return prevention
34. **T3**: Return after shift closure
35. **T4**: Return of controlled medicine
36. **T5**: Return quarantine verification

### Group 8: DATA CONSISTENCY (Medium)
37. **D1**: Sales vs payments reconciliation
38. **D2**: Inventory vs movement journal reconciliation
39. **D3**: Cash drawer vs transactions reconciliation
40. **D4**: Audit trail completeness
41. **D5**: Prescription vs dispensing reconciliation

### Group 9: DEPLOYMENT RESILIENCE (Medium)
42. **L1**: Backup and restore cycle
43. **L2**: Health endpoint accuracy
44. **L3**: Startup failure matrix
45. **L4**: LAN security (unauthenticated access)

### Group 10: STRESS (Low)
46. **X1**: 100 sequential sales
47. **X2**: 50 concurrent operations
48. **X3**: Long-running session behavior

## Release Gates

### BLOCK RELEASE
- Duplicate financial transaction
- Lost financial transaction
- Incorrect payment state
- Negative stock caused by concurrency
- Tenant data leakage
- Unauthorized financial action
- Corrupted shift balance
- Failed backup/restore
- Silent migration corruption
- Forged payment accepted
- Orphan payment
- Orphan stock deduction
- Inconsistent sale/payment totals

### ACCEPTABLE WITH DOCUMENTATION
- Missing advanced analytics
- Missing WhatsApp integration
- Missing Airtel/Equity integration
- Cosmetic UI issues
- Non-critical reporting enhancements
- M-Pesa callback endpoints disabled (documented)
- Offline mode not implemented (documented)

## Execution

```powershell
# Run full brutal suite
.\scripts\brutal-runner.ps1

# Run specific group
.\scripts\brutal-runner.ps1 -Group Concurrency

# Run with chaos seed
.\scripts\brutal-runner.ps1 -ChaosSeed 184729

# Run existing regression first
.\scripts\brutal-runner.ps1 -IncludeRegression
```
