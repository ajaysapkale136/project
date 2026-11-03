package com.aiproject.architect.common;

/**
 * 4-state confidence model ensuring strict provenance for all discovered
 * technologies, architectural components, dependencies, and business rules.
 */
public enum ConfidenceState {
    /** Discovered via deterministic AST parser, framework annotations, or explicit schema definitions */
    VERIFIED,

    /** Derived via call-graph analysis, import tracing, or heuristic pattern analysis */
    INFERRED,

    /** Proposed by AI models, comments, or unconfirmed hints lacking direct syntactic proof */
    UNVERIFIED,

    /** Conflicting evidence detected between declared contract and actual implementation */
    CONFLICTING
}
