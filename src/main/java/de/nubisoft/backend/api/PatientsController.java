package de.nubisoft.backend.api;


import de.nubisoft.backend.domain.PatientDocumentsResponse;
import de.nubisoft.backend.service.PatientsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
public class PatientsController {

    private final PatientsService patientsService;

    @Autowired
    public PatientsController(PatientsService patientsService) {
        this.patientsService = patientsService;
    }

    @GetMapping("/{id}/documents/v1")
    PatientDocumentsResponse getPatientDocumentsV1(@PathVariable String id) {
        return this.patientsService.getPatientDocumentsV1(id);
    }

    @GetMapping("/{id}/documents/v2")
    PatientDocumentsResponse getPatientDocumentsV2(@PathVariable String id) {
        return this.patientsService.getPatientDocumentsV2(id);
    }

    @GetMapping("/{id}/documents/v3")
    PatientDocumentsResponse getPatientDocumentsV3(@PathVariable String id) {
        return this.patientsService.getPatientDocumentsV3(id);
    }

    @GetMapping("/{id}/documents/v4")
    PatientDocumentsResponse getPatientDocumentsV4(@PathVariable String id) {
        return this.patientsService.getPatientDocumentsV4(id);
    }

    @GetMapping("/{id}/documents/v5")
    PatientDocumentsResponse getPatientDocumentsV5(@PathVariable String id) {
        return this.patientsService.getPatientDocumentsV5(id);
    }

    @GetMapping("/{id}/documents/v6")
    PatientDocumentsResponse getPatientDocumentsV6(@PathVariable String id) {
        return this.patientsService.getPatientDocumentsV6(id);
    }
}
