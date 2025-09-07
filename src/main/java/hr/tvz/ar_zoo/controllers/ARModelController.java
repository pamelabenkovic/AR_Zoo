package hr.tvz.ar_zoo.controllers;

import hr.tvz.ar_zoo.models.ARModel;
import hr.tvz.ar_zoo.services.ARModelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/ar-models")
public class ARModelController {

    private final ARModelService service;

    public ARModelController(ARModelService service) {
        this.service = service;
    }

    @GetMapping
    public List<ARModel> getAll() {
        return service.getAllModels();
    }

    @GetMapping("/{modelId}")
    public ARModel getByModelId(@PathVariable String modelId) {
        return service.getByModelId(modelId);
    }

    @PostMapping
    public ARModel createModel(@RequestBody ARModel model) {
        return service.saveModel(model);
    }
}
