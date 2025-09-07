package hr.tvz.ar_zoo.services;


import hr.tvz.ar_zoo.models.ARModel;
import hr.tvz.ar_zoo.repositories.ARModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ARModelService {

    private final ARModelRepository repository;

    public ARModelService(ARModelRepository repository) {
        this.repository = repository;
    }

    public List<ARModel> getAllModels() {
        return repository.findAll();
    }

    public ARModel getByModelId(String modelId) {
        return repository.findByModelId(modelId);
    }

    public ARModel saveModel(ARModel model) {
        return repository.save(model);
    }
}
