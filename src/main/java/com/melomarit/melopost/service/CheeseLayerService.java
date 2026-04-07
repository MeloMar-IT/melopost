package com.melomarit.melopost.service;

import com.melomarit.melopost.model.CheeseLayer;
import com.melomarit.melopost.repository.CheeseLayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CheeseLayerService {
    @Autowired
    private CheeseLayerRepository repository;

    public List<CheeseLayer> findAll() { return repository.findAll(); }
    public Optional<CheeseLayer> findById(Long id) { return repository.findById(id); }
    public CheeseLayer save(CheeseLayer layer) { return repository.save(layer); }
    public void deleteById(Long id) { repository.deleteById(id); }
}
