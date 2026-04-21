package com.melomarit.melopost.dto;

import com.melomarit.melopost.model.Postmortem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostmortemSearchResultDTO {
    private Postmortem postmortem;
    private List<String> matchHints = new ArrayList<>();

    public Postmortem getPostmortem() {
        return postmortem;
    }

    public void setPostmortem(Postmortem postmortem) {
        this.postmortem = postmortem;
    }

    public List<String> getMatchHints() {
        return matchHints;
    }

    public void setMatchHints(List<String> matchHints) {
        this.matchHints = matchHints;
    }

    public PostmortemSearchResultDTO(Postmortem postmortem) {
        this.postmortem = postmortem;
    }

    public void addHint(String hint) {
        if (!matchHints.contains(hint)) {
            matchHints.add(hint);
        }
    }
}
