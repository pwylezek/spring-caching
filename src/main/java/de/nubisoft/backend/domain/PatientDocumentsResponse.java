package de.nubisoft.backend.domain;

import java.util.List;

public record PatientDocumentsResponse(Patient patient, List<String> documents) {
}
