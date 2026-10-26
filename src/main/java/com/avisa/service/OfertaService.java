package com.avisa.service;

import com.avisa.dto.OfertaRequest;
import com.avisa.model.Oferta;
import com.avisa.model.Tasca;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió d'ofertes de serveis publicades per professionals.
 * Suporta el flux de contractació directa persistent i acceptació d'ofertes per generar tasques.
 */
@Service
public class OfertaService {

	private final InMemoryCatalog catalog;

	public OfertaService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les ofertes registrades a la plataforma.
	 * @return Llista completa d'ofertes
	 */
	public List<Oferta> list() {
		return catalog.offers();
	}

	/**
	 * Troba una oferta pel seu identificador.
	 * @param id Identificador de l'oferta
	 * @return Oferta si existeix
	 */
	public Optional<Oferta> findById(String id) {
		return catalog.findOfferById(id);
	}

	/**
	 * Llistar totes les ofertes associades a una tasca concreta.
	 * @param tascaId Identificador de la tasca
	 * @return Llista d'ofertes vinculades
	 */
	public List<Oferta> listByTasca(String tascaId) {
		return catalog.findOffersByTasca(tascaId);
	}

	/**
	 * Llistar les ofertes presentades per un professional concret.
	 * @param professionalNom Nom del professional
	 * @return Llista d'ofertes enviades
	 */
	public List<Oferta> listByProfessional(String professionalNom) {
		return catalog.offers().stream()
			.filter(o -> o.professional() != null && o.professional().equalsIgnoreCase(professionalNom))
			.toList();
	}

	/**
	 * Crear una nova oferta de servei o pressupost.
	 * @param request Dades de l'oferta
	 * @return L'oferta creada
	 */
	public Oferta create(OfertaRequest request) {
		return catalog.addOffer(request);
	}

	/**
	 * Accepta una oferta vinculada a una tasca existent.
	 * @param offerId Identificador de l'oferta
	 * @return Oferta acceptada
	 */
	public Optional<Oferta> accept(String offerId) {
		return catalog.acceptOffer(offerId);
	}

	/**
	 * Contractació directa d'una oferta pública per part d'un client:
	 * Genera una tasca en curs privada mantenint l'oferta activa al mercat per a futurs clients.
	 * @param offerId Identificador de l'oferta contractada
	 * @param clientNom Nom o identificador del client contractant
	 * @return La tasca en curs creada
	 */
	public Optional<Tasca> contractDirect(String offerId, String clientNom) {
		return catalog.convertOfferToTask(offerId, clientNom);
	}

	/**
	 * Elimina una oferta del mercat.
	 * @param id Identificador de l'oferta
	 * @return true si s'ha esborrat correctament
	 */
	public boolean delete(String id) {
		return catalog.deleteOffer(id);
	}

	/**
	 * Rebutja una oferta.
	 * @param offerId Identificador de l'oferta
	 * @return Oferta actualitzada si s'ha rebutjat
	 */
	public Optional<Oferta> reject(String offerId) {
		return catalog.rejectOffer(offerId);
	}
}

