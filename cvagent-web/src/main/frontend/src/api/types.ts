// ===== Entities (matching backend Java entities) =====

export interface WorkExperience {
  id: number;
  personName: string;
  personEmail: string;
  personPhone: string;
  summary: string;
  skills: string;
  professionalExp: string;
  education: string;
  otherInfo: string;
  rawFileName: string;
  rawFileType: string;
  rawContent: string;
  createdAt: string;
  updatedAt: string;
}

export interface CvTemplate {
  id: number;
  name: string;
  description: string;
  templateContent: string;
  isPreset: boolean;
  fileName: string;
  createdAt: string;
  updatedAt: string;
}

export interface JobDescription {
  id: number;
  title: string;
  company: string;
  content: string;
  rawFileName: string;
  rawFileType: string;
  createdAt: string;
  updatedAt: string;
}

export type CvStatus = 'DRAFT' | 'FINAL' | 'EXPORTED';

export type ScoringStatus = 'SCORING' | 'COMPLETED' | 'FAILED';

export interface GeneratedCv {
  id: number;
  workExpId: number;
  templateId: number;
  finalContent: string;
  status: CvStatus;
  createdAt: string;
  updatedAt: string;
  // transient display fields (not persisted, filled by list API)
  workExpName?: string;
  templateName?: string;
}

export interface CvScoringResult {
  id: number;
  generatedCvId: number;
  jdId: number;
  finalScore: number | null;
  finalFeedback: string | null;
  roleScores: string | null; // JSON string
  iterationCount: number;
  status: ScoringStatus;
  createdAt: string;
  updatedAt: string;
  jdTitle?: string;
}

export interface CvGenerationRecord {
  id: number;
  generatedCvId: number;
  iteration: number;
  roleScores: string; // JSON string
  overallScore: number;
  feedback: string;
  cvSnapshot: string;
  createdAt: string;
}

// ===== Request DTOs =====

export interface CvGenerateRequest {
  workExpId: number;
  templateId: number;
  jdId?: number;
}

export interface CvContentUpdateRequest {
  finalContent: string;
}

// ===== Response DTOs =====

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  pages: number; // computed by Math.ceil(total / size)
}

export interface ApiErrorResponse {
  code: number;
  message: string;
  timestamp: number;
  path?: string;
}

// ===== Parsed role scores =====
export type RoleScores = Record<string, number>;

export function parseRoleScores(roleScoresJson: string): RoleScores {
  try {
    return JSON.parse(roleScoresJson);
  } catch {
    return {};
  }
}
