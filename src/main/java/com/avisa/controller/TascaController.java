package com.avisa.controller;

import com.avisa.dto.TascaRequest;
import com.avisa.model.Tasca;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:4200")
public class TascaController {

	private final InMemoryCatalog catalog;

	public TascaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public List<Tasca> list() {
		return catalog.tasks();
	}

	@PostMapping
	public Tasca create(@RequestBody TascaRequest request) {
		return catalog.addTask(request);
	}
}
