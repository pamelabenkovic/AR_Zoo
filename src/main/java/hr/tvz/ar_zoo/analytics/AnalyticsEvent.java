package hr.tvz.ar_zoo.analytics;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("analytics_events")
public class AnalyticsEvent {
    @Id private String id;
    private String type;
    private String modelId;
    private String modelName;
    private String page;
    private String clientId;
    private Instant timestamp;

    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getType(){return type;} public void setType(String type){this.type=type;}
    public String getModelId(){return modelId;} public void setModelId(String modelId){this.modelId=modelId;}
    public String getModelName(){return modelName;} public void setModelName(String modelName){this.modelName=modelName;}
    public String getPage(){return page;} public void setPage(String page){this.page=page;}
    public String getClientId(){return clientId;} public void setClientId(String clientId){this.clientId=clientId;}
    public Instant getTimestamp(){return timestamp;} public void setTimestamp(Instant timestamp){this.timestamp=timestamp;}
}
