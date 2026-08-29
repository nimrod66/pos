export interface Insurer {
  id: string;
  name: string;
  code: string;
  insurerType: string;
  contactPerson: string | null;
  phoneNumber: string | null;
  email: string | null;
  claimSubmissionEmail: string | null;
  preauthPhone: string | null;
  defaultCoPayPercentage: number | null;
  defaultCoPayFlat: number | null;
  requiresPreauth: boolean;
  maxClaimAmount: number | null;
  status: string | null;
}

export interface InsuranceClaim {
  id: string;
  insurerId: string | null;
  insurerName: string | null;
  schemeId: string | null;
  schemeName: string | null;
  memberId: string | null;
  memberName: string | null;
  authorizationId: string | null;
  authorizationRef: string | null;
  batchId: string | null;
  batchRef: string | null;
  paymentId: string | null;
  paymentRef: string | null;
  saleId: string | null;
  patientName: string | null;
  patientMembershipId: string | null;
  claimAmount: number | null;
  approvedAmount: number | null;
  rejectedAmount: number | null;
  coPayAmount: number | null;
  saleTotal: number | null;
  claimReference: string | null;
  claimStatus: string | null;
  submittedAt: string | null;
  rejectionReason: string | null;
  notes: string | null;
}

export interface InsuranceMember {
  id: string;
  insurerId: string;
  memberName: string;
  membershipNumber: string;
  nationalId: string | null;
  phoneNumber: string | null;
  dateOfBirth: string | null;
  status: string | null;
  expiryDate: string | null;
}

export interface CreateInsurerInput {
  name: string;
  code: string;
  insurerType: string;
  contactPerson?: string;
  phoneNumber?: string;
  email?: string;
  claimSubmissionEmail?: string;
  preauthPhone?: string;
  defaultCoPayPercentage?: number;
  defaultCoPayFlat?: number;
  requiresPreauth?: boolean;
  maxClaimAmount?: number;
  status?: string;
}

export interface InsuranceScheme {
  id: string;
  insurerId: string;
  insurerName: string | null;
  name: string;
  code: string;
  schemeType: string | null;
  coPayPercentage: number | null;
  coPayFlat: number | null;
  maxClaimAmount: number | null;
  requiresPreauth: boolean;
  status: string | null;
}

export interface CreateSchemeInput {
  name: string;
  code: string;
  schemeType?: string;
  coPayPercentage?: number;
  coPayFlat?: number;
  maxClaimAmount?: number;
  requiresPreauth?: boolean;
}

export interface Authorization {
  id: string;
  insurerId: string | null;
  insurerName: string | null;
  memberId: string | null;
  memberName: string | null;
  membershipNumber: string | null;
  authorizationCode: string | null;
  authorizedAmount: number | null;
  usedAmount: number | null;
  remainingAmount: number | null;
  validFrom: string | null;
  validTo: string | null;
  status: string | null;
  notes: string | null;
}

export interface CreateAuthorizationInput {
  memberId?: string;
  authorizationCode?: string;
  authorizedAmount?: number;
  validFrom?: string;
  validTo?: string;
  notes?: string;
}

export interface ClaimBatch {
  id: string;
  insurerId: string | null;
  insurerName: string | null;
  batchReference: string | null;
  claimCount: number | null;
  totalAmount: number | null;
  approvedAmount: number | null;
  paidAmount: number | null;
  status: string | null;
  submittedAt: string | null;
  createdAt: string | null;
}

export interface CreateBatchInput {
  claimIds?: string[];
  notes?: string;
}

export interface InsurancePayment {
  id: string;
  insurerId: string | null;
  insurerName: string | null;
  batchId: string | null;
  batchRef: string | null;
  paymentReference: string | null;
  paymentAmount: number | null;
  paymentDate: string | null;
  paymentMethod: string | null;
  linkedClaimCount: number | null;
  status: string | null;
  notes: string | null;
  createdAt: string | null;
}

export interface CreatePaymentInput {
  batchId?: string;
  paymentReference?: string;
  paymentAmount?: number;
  paymentDate?: string;
  paymentMethod?: string;
  notes?: string;
}

export interface Reconciliation {
  id: string;
  insurerId: string | null;
  insurerName: string | null;
  periodFrom: string | null;
  periodTo: string | null;
  totalClaims: number | null;
  totalClaimedAmount: number | null;
  totalApprovedAmount: number | null;
  totalPaidAmount: number | null;
  outstandingAmount: number | null;
  status: string | null;
  createdAt: string | null;
}

export interface InsurerReport {
  insurerId: string;
  insurerName: string;
  totalClaims: number;
  totalClaimedAmount: number;
  totalApprovedAmount: number;
  totalPaidAmount: number;
  totalRejectedAmount: number;
  outstandingAmount: number;
  claimsByStatus: Record<string, number>;
}
