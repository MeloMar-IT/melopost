package com.melomarit.melopost.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class PostmortemQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;
    private String answer;
    private String cheeseLayer;

    @ManyToOne
    @JoinColumn(name = "postmortem_id")
    @JsonIgnore
    private Postmortem postmortem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCheeseLayer() { return cheeseLayer; }
    public void setCheeseLayer(String cheeseLayer) { this.cheeseLayer = cheeseLayer; }
    public Postmortem getPostmortem() { return postmortem; }
    public void setPostmortem(Postmortem postmortem) { this.postmortem = postmortem; }
}
