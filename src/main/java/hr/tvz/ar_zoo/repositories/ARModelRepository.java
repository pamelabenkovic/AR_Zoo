package hr.tvz.ar_zoo.repositories;

import hr.tvz.ar_zoo.models.ARModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ARModelRepository extends MongoRepository<ARModel, String> {
    ARModel findByModelId(String modelId);
}
