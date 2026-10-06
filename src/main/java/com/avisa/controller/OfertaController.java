package com.avisa.controller;

import com.avisa.dto.OfertaRequest;
import com.avisa.model.Oferta;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "http://localhost:4200")
public class OfertaController {

	private final InMemoryCatalog catalog;

	public OfertaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public List<Oferta> list() {
		return catalog.offers();
	}

	@PostMapping
	public Oferta create(@RequestBody OfertaRequest request) {
		return catalog.addOffer(request);
	}
}
