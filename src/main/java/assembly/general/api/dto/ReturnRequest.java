package assembly.general.api.dto;

import assembly.general.api.entity.BookCondition;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnRequest {
    private BookCondition condition;
    private String notes;
}