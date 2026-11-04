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
	 * Llistar les ofertes presentades per un professional concret.
	 * @param professionalNom Nom del professional
	 * @return Llista d'ofertes enviades
	 */
	public List<Oferta> listByProfessional(String professionalNom) {
		if (professionalNom == null || professionalNom.isBlank()) {
			return List.of();
		}
		return catalog.offers().stream()
			.filter(o -> (o.professional() != null && o.professional().equalsIgnoreCase(professionalNom))
				|| (o.professionalId() != null && o.professionalId().equalsIgnoreCase(professionalNom)))
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
	 * Sol·licitud d'una oferta per part d'un client ("M'interessa"):
	 * queda pendent del professional i l'oferta continua activa per a altres clients.
	 * @param offerId Identificador de l'oferta
	 * @param clientNom Identificador del client
	 * @return La sol·licitud creada
	 */
	public Optional<Tasca> contractDirect(String offerId, String clientNom) {
		return catalog.convertOfferToTask(offerId, clientNom);
	}

	/**
	 * Elimina una oferta del mercat.
	 * @param id Identificador de l'oferta
	 * @param actorId Usuari que fa l'acció
	 * @return true si l'oferta existeix i ha quedat retirada
	 */
	public boolean delete(String id, String actorId) {
		return catalog.deleteOffer(id, actorId);
	}
}

