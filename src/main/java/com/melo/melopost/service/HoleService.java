package com.melo.melopost.service;

import com.melo.melopost.model.Hole;
import com.melo.melopost.repository.HoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HoleService {
    @Autowired
    private HoleRepository repository;

    public List<Hole> findAll() { return repository.findAll(); }
    public Optional<Hole> findById(Long id) { return repository.findById(id); }
    public Hole save(Hole hole) { return repository.save(hole); }
    public void deleteById(Long id) { repository.deleteById(id); }
}
