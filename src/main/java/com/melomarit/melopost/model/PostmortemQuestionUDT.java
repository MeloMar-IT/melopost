package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.Data;
import java.util.UUID;

@Data
@UserDefinedType("postmortem_question")
public class PostmortemQuestionUDT {
    private UUID uuid = UUID.randomUUID();
    private String question;
    private String answer;
    @Column("cheeselayer")
    private String cheeseLayer;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCheeseLayer() { return cheeseLayer; }
    public void setCheeseLayer(String cheeseLayer) { this.cheeseLayer = cheeseLayer; }
}
