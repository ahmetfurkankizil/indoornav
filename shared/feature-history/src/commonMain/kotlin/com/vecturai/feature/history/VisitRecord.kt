package com.vecturai.feature.history

// VisitRecord is canonically defined in core.domain to avoid circular dependency
// (core ← feature-history would be circular; feature-history depends on core).
// This typealias keeps existing feature code compiling unchanged.
typealias VisitRecord = com.vecturai.core.domain.VisitRecord

