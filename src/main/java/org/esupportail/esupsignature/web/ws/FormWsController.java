package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.esupportail.esupsignature.dto.RecipientWsDto;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.dto.WorkflowDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws/forms")
public class FormWsController {

    private static final Logger logger = LoggerFactory.getLogger(FormWsController.class);

    @Resource
    private DataService dataService;

    @Resource
    private FormService formService;

    @Resource
    private RecipientService recipientService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private DataExportService dataExportService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private ObjectMapper objectMapper;

    @CrossOrigin
    @PostMapping(value = "/{id}/new", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Création d'une nouvelle instance d'un formulaire")
    public ResponseEntity<?> start(@PathVariable Long id,
                                   @RequestParam(required = false) @Parameter(description = "Paramètres des étapes (objet json)", array = @ArraySchema(schema = @Schema( implementation = WorkflowStepDto.class)), example = "[{\n" +
                                           "  \"title\": \"string\",\n" +
                                           "  \"workflowId\": 0,\n" +
                                           "  \"stepNumber\": 0,\n" +
                                           "  \"description\": \"string\",\n" +
                                           "  \"recipientsCCEmails\": [\n" +
                                           "    \"string\"\n" +
                                           "  ],\n" +
                                           "  \"recipients\": [\n" +
                                           "    {\n" +
                                           "      \"step\": 0,\n" +
                                           "      \"email\": \"string\",\n" +
                                           "      \"phone\": \"string\",\n" +
                                           "      \"name\": \"string\",\n" +
                                           "      \"firstName\": \"string\",\n" +
                                           "      \"forceSms\": true\n" +
                                           "    }\n" +
                                           "  ],\n" +
                                           "  \"changeable\": true,\n" +
                                           "  \"signLevel\": 0,\n" +
                                           "  \"signType\": \"hiddenVisa\",\n" +
                                           "  \"repeatable\": true,\n" +
                                           "  \"repeatableSignType\": \"hiddenVisa\",\n" +
                                           "  \"allSignToComplete\": true,\n" +
                                           "  \"userSignFirst\": true,\n" +
                                           "  \"multiSign\": true,\n" +
                                           "  \"autoSign\": true,\n" +
                                           "  \"forceAllSign\": true,\n" +
                                           "  \"comment\": \"string\",\n" +
                                           "  \"attachmentRequire\": true,\n" +
                                           "  \"maxRecipients\": 0\n" +
                                           "}]") String stepsJsonString,
                                   @RequestParam(required = false) @Parameter(description = "EPPN du créateur/propriétaire de la demande") String createByEppn,
                                   @RequestParam(required = false) @Parameter(description = "Titre (facultatif)") String title,
                                   @RequestParam(required = false) @Parameter(description = "Liste des personnes en copie (emails). Ne prend pas en charge les groupes")  List<String> recipientsCCEmails,
                                   @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux (emails)", example = "[email]") List<String> targetEmails,
                                   @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                                   @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                                   @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Envoyer une alerte mail") Boolean sendEmailAlert,
                                   @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants pour chaque étape (ancien nom)", example = "[stepNumber*email] ou [stepNumber*email*phone]") List<String> recipientEmails,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants pour chaque étape", example = "[stepNumber*email] ou [stepNumber*email*phone]") List<String> recipientsEmails,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des types de signature pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Eppn du propriétaire du futur document (ancien nom)") String eppn


    ) {
        logger.debug("init new form instance : " + id);
        if(json == null) {
            json = false;
        }
        if(recipientEmails == null && recipientsEmails != null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        if(stepsJsonString == null && recipientEmails != null) {
            stepsJsonString = recipientService.convertRecipientEmailsToStep(recipientEmails).toString();
        }
        if(createByEppn == null && eppn != null && !eppn.isEmpty()) {
            createByEppn = eppn;
        }
        if(createByEppn == null) {
            throw new EsupSignatureRuntimeException("Required request parameter 'createByEppn' for method parameter type String is not present");
        }
        try {
            Data data = dataService.addData(id, createByEppn);
            TypeReference<Map<String, String>> type = new TypeReference<>(){};
            Map<String, String> datas = new HashMap<>();
            if(formDatas != null) {
                datas = objectMapper.readValue(formDatas, type);
            }
            SignBook signBook = signBookService.sendForSign(data.getId(), recipientService.convertRecipientJsonStringToWorkflowStepDtos(stepsJsonString), targetEmails, targetUrls, createByEppn, createByEppn, true, datas, null, signRequestParamsJsonString, title, sendEmailAlert);
            signBookService.addViewers(signBook.getId(), recipientsCCEmails);
            if(json) {
                return ResponseEntity.ok(signBook.getSignRequests().get(0).getId());
            } else {
                return ResponseEntity.ok(signBook.getSignRequests().get(0).getId().toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.ok("-1");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération d'un circuit", responses = @ApiResponse(description = "JsonDtoWorkflow", content = @Content(schema = @Schema(implementation = WorkflowDto.class))))
    public String get(@PathVariable Long id) throws JsonProcessingException {
        return formService.getByIdJson(id);
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/new-doc")
    @Operation(description = "Création d'une nouvelle instance d'un formulaire")
    public ResponseEntity<?> startWithDoc(@PathVariable Long id,
                             @RequestParam @Parameter(description = "Multipart stream du fichier à signer") MultipartFile[] multipartFiles,
                             @RequestParam @Parameter(description = "Eppn du propriétaire du futur document") String createByEppn,
                             @RequestParam(required = false) @Parameter(description = "Multipart stream des pièces jointes") MultipartFile[] attachementMultipartFiles,
                             @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape (objet json)", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RecipientWsDto.class)))) List<WorkflowStepDto> steps,
                             @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape (ancien nom)", example = "[stepNumber*email]") List<String> recipientEmails,
                             @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*email]") List<String> recipientsEmails,
                             @RequestParam(required = false) @Parameter(description = "Liste des types de signature pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                             @RequestParam(required = false) @Parameter(description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                             @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                             @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                             @RequestParam(required = false) @Parameter(description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                             @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                             @RequestParam(required = false) @Parameter(description = "Titre") String title,
                             @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Envoyer une alerte mail") Boolean sendEmailAlert,
                             @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json
    ) {
        if(json == null) {
            json = false;
        }
        if(recipientEmails == null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        if(steps == null && recipientEmails != null) {
            steps = recipientService.convertRecipientEmailsToStep(recipientEmails);
        }
        Data data = dataService.addData(id, createByEppn);
        try {
            TypeReference<Map<String, String>> type = new TypeReference<>(){};
            Map<String, String> datas = new HashMap<>();
            if(formDatas != null) {
                datas.putAll(objectMapper.readValue(formDatas, type));
            }
            SignBook signBook = signBookService.sendForSign(data.getId(), steps, targetEmails, targetUrls, createByEppn, createByEppn, true, datas, multipartFiles[0].getInputStream(), signRequestParamsJsonString, title, sendEmailAlert);
            signRequestService.addAttachement(attachementMultipartFiles, null, signBook.getSignRequests().get(0).getId(), createByEppn);
            if(json) {
                return ResponseEntity.ok(signBook.getSignRequests().get(0).getId());
            } else {
                return ResponseEntity.ok(signBook.getSignRequests().get(0).getId().toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.ok("-1");
        }
    }

    @CrossOrigin
    @PostMapping(value = "/get-datas/{id}")
    @Operation(description = "Récupération des données d'un formulaire (POST)")
    public LinkedHashMap<String, String> postGetDatas(@PathVariable Long id) {
        return dataExportService.getJsonDatasFromSignRequest(id);
    }

    @CrossOrigin
    @GetMapping(value = "/get-datas/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération des données d'un formulaire")
    public LinkedHashMap<String, String> getDatas(@PathVariable Long id) {
        return dataExportService.getJsonDatasFromSignRequest(id);
    }

}
