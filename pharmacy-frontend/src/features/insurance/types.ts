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
