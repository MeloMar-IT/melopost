package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Data
@UserDefinedType("cheese_layer")
public class CheeseLayerUDT {
    private UUID uuid = UUID.randomUUID();
    private String name;
    private String description;
    private List<HoleUDT> holes = new ArrayList<>();

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<HoleUDT> getHoles() { return holes; }
    public void setHoles(List<HoleUDT> holes) { this.holes = holes; }
}
