package com.melo.melopost.service;

import com.melo.melopost.model.CheeseLayer;
import com.melo.melopost.repository.CheeseLayerRepository;
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
