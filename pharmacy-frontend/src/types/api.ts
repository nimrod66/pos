export interface ApiResponseMeta {
  requestId: string;
}

export interface ApiResponse<T> {
  data: T;
  meta: ApiResponseMeta;
}

export interface BackendApiResponse<T> {
  success: true;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface ApiFieldError {
  field: string;
  code: string;
  message: string;
}

export interface ApiErrorResponse {
  error: {
    code: string;
    message: string;
    fieldErrors: ApiFieldError[];
  };
  meta: ApiResponseMeta & {
    timestamp: string;
  };
}

export interface BackendApiErrorResponse {
  success: false;
  message: string;
  errorCode?: string;
  status?: number;
  path?: string;
  timestamp?: string;
  validationErrors?: Array<{
    field: string;
    message: string;
  }>;
}

export interface SystemStatus {
  application: string;
  api: "UP";
  database: "UP";
  databaseName: string;
  version: string;
  checkedAt: string;
}
