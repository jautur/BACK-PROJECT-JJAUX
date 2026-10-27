-- ==============================================================================
-- Script SQL: Substitució de Professionals per Empreses amb Foto de Perfil i Logo
-- Base de Dades: jjaux_db (PostgreSQL) - Plataforma AVISA
-- Executa un DELETE dels antics professionals individuals i un INSERT
-- de les noves empreses homologades amb foto de perfil (isotip) i logo (fons targeta).
-- ==============================================================================

-- 1. Assegurar l'existència de les columnes foto_perfil i logo a la taula usuaris
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS foto_perfil TEXT;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS logo TEXT;

-- 2. DELETE: Neteja prèvia de registres vinculats als antics professionals individuals
DELETE FROM resenyes 
WHERE treballador_id IN ('u-pro-vicent', 'u-pro-elena', 'u-pro-marc', 'u-pro-laura', 'u-pro-carles', 'u-2', 'u-3')
   OR professional_nom IN ('Vicent Fontanals (Lampisteria)', 'Elena Martínez (Electricitat)', 'Marc Soler (Pintura i Decoració)', 'Laura Beneyto (Climatització)', 'Carles Ribes (Fusteria)', 'Jordi Romero', 'Clara Sanz');

DELETE FROM ofertes 
WHERE professional IN ('Vicent Fontanals (Lampisteria)', 'Elena Martínez (Electricitat)', 'Marc Soler (Pintura i Decoració)', 'Laura Beneyto (Climatització)', 'Carles Ribes (Fusteria)', 'Jordi Romero', 'Clara Sanz', 'Pau Ferrer');

DELETE FROM publicacions 
WHERE usuari_id IN ('u-pro-vicent', 'u-pro-elena', 'u-pro-marc', 'u-pro-laura', 'u-pro-carles', 'u-2', 'u-3')
   OR usuari_nom IN ('Vicent Fontanals (Lampisteria)', 'Elena Martínez (Electricitat)', 'Marc Soler (Pintura i Decoració)', 'Laura Beneyto (Climatització)', 'Carles Ribes (Fusteria)', 'Jordi Romero', 'Clara Sanz');

DELETE FROM pressupostos 
WHERE professional_id IN ('u-pro-vicent', 'u-pro-elena', 'u-pro-marc', 'u-pro-laura', 'u-pro-carles', 'u-2', 'u-3')
   OR professional_nom IN ('Vicent Fontanals (Lampisteria)', 'Elena Martínez (Electricitat)', 'Marc Soler (Pintura i Decoració)', 'Laura Beneyto (Climatització)', 'Carles Ribes (Fusteria)', 'Jordi Romero', 'Clara Sanz');

DELETE FROM tasques 
WHERE professional_id IN ('u-pro-vicent', 'u-pro-elena', 'u-pro-marc', 'u-pro-laura', 'u-pro-carles', 'u-2', 'u-3');

DELETE FROM usuaris 
WHERE id IN ('u-pro-vicent', 'u-pro-elena', 'u-pro-marc', 'u-pro-laura', 'u-pro-carles', 'u-2', 'u-3', 'u-pro-fontanals', 'u-pro-electromartinez', 'u-pro-solerpintures', 'u-pro-climabeneyto', 'u-pro-fusteriaribes', 'u-pro-llevant');

-- 3. INSERT: Inserció de les noves empreses homologades amb foto_perfil (isotip) i logo (fons de targeta)
INSERT INTO usuaris (
    id, nom, email, password, rol, telefon, bio, percentil, verificat, sollicitud_verificacio, foto, foto_perfil, logo
) VALUES 
(
    'u-pro-fontanals',
    'Fontanals Lampisteria & Climatització S.L.',
    'contacte@fontanals.es',
    '123456',
    'professional',
    '963112233',
    'Empresa líder en lampisteria urgent 24h, reparació de canonades, descalcificadors, termos elèctrics i calderes a València i àrea metropolitana. Més de 15 anys de garantia i resposta immediata.',
    5,
    TRUE,
    FALSE,
    'https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=300&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800&auto=format&fit=crop&q=80'
),
(
    'u-pro-electromartinez',
    'ElectroMartínez Instal·lacions Elèctriques S.L.',
    'info@electromartinez.es',
    '123456',
    'professional',
    '963223344',
    'Enginyeria i muntatges elèctrics autoritzats. Quadres industrials, domòtica domèstica, panells solars fotovoltaics, butlletins oficials i punts de recàrrega de vehicles elèctrics.',
    4,
    TRUE,
    FALSE,
    'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1560179707-f14e90ef3623?w=300&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&auto=format&fit=crop&q=80'
),
(
    'u-pro-solerpintures',
    'Soler Pintura & Alta Decoració S.L.',
    'info@solerpintura.es',
    '123456',
    'professional',
    '963334455',
    'Empresa familiar amb 25 anys d''experiència en alta decoració, pintura plàstica ecològica, allisats de parets, microciment continu i impermeabilització de façanes i terrasses.',
    4,
    TRUE,
    FALSE,
    'https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1572021335469-31706a17aaef?w=300&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80'
),
(
    'u-pro-climabeneyto',
    'ClimaBeneyto Climatització & Aerotèrmia S.L.',
    'serveis@climabeneyto.es',
    '123456',
    'professional',
    '963556677',
    'Servei tècnic oficial multimarca. Instal·lació i manteniment d''aire condicionat, bombes de calor aerotèrmiques d''alta eficiència A+++, terra radiant i sistemes de ventilació mecànica.',
    5,
    TRUE,
    FALSE,
    'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800&auto=format&fit=crop&q=80'
),
(
    'u-pro-fusteriaribes',
    'Fusteria Artesana Ribes & Fills S.L.',
    'tallers@fusteriaribes.es',
    '123456',
    'professional',
    '963667788',
    'Fusteria a mida i ebenisteria arquitectònica. Fabricació pròpia de cuines de disseny, portes acústiques, vestidors integrats i restauració de fustes nobles per a vivendes i negocis.',
    3,
    FALSE,
    TRUE,
    'https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=300&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800&auto=format&fit=crop&q=80'
),
(
    'u-pro-llevant',
    'Llevant Reformes & Obres Integrals S.L.',
    'projectes@llevantreformes.es',
    '123456',
    'professional',
    '963778899',
    'Empresa constructora homologada per a reformes integrals de vivendes i locals comercials. Projectes clau en mà amb direcció facultativa, paletes, enrajolats i coordinació de gremis.',
    4,
    TRUE,
    FALSE,
    'https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1497366216548-37526070297c?w=300&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80'
);

-- Actualització del compte professional base 'u-pro' per reflectir la nova identitat corporativa
UPDATE usuaris 
SET nom = 'Llevant Reformes & Obres Integrals S.L.',
    bio = 'Empresa constructora homologada per a reformes integrals de vivendes i locals comercials. Projectes clau en mà amb direcció facultativa, paletes, enrajolats i coordinació de gremis.',
    foto = 'https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80',
    foto_perfil = 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=300&auto=format&fit=crop&q=80',
    logo = 'https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80',
    verificat = TRUE,
    sollicitud_verificacio = FALSE
WHERE id = 'u-pro';

-- 4. INSERT d'ofertes vinculades a les noves empreses
INSERT INTO ofertes (id, professional, price, note, categoria, estat) VALUES 
('O-101', 'Fontanals Lampisteria & Climatització S.L.', '45€ / hora', 'Reparació urgent de fuites d''aigua, desbussament d''aigüeres, canvi d''aixetes i manteniment de calderes. Arribada en menys de 45 minuts.', 'lampisteria', 'Pendent'),
('O-102', 'ElectroMartínez Instal·lacions Elèctriques S.L.', '60€', 'Diagnòstic de curtcircuits, canvi de diferencials i revisió reglamentària del quadre elèctric amb garantia oficial.', 'electricitat', 'Pendent'),
('O-103', 'Soler Pintura & Alta Decoració S.L.', '350€', 'Pintura completa de saló o habitació amb pintura rentada d''alta cobertura i protecció total de mobles i sòl.', 'pintura', 'Pendent'),
('O-104', 'Fusteria Artesana Ribes & Fills S.L.', '120€', 'Ajustament de portes que freguen, raspallat, canvi de panys de seguretat i reparació de calaixos encallats.', 'fusteria', 'Pendent'),
('O-105', 'ClimaBeneyto Climatització & Aerotèrmia S.L.', '85€', 'Neteja profunda de filtres, desinfecció antibacteriana i revisió de pressió de gas per a equips domèstics d''aire condicionat.', 'climatitzacio', 'Pendent'),
('O-106', 'Llevant Reformes & Obres Integrals S.L.', '95€', 'Treballs ràpids d''obra menor, enrajolat de zones humides i petites reparacions de paleta per a la llar.', 'reformes', 'Pendent')
ON CONFLICT (id) DO UPDATE 
SET professional = EXCLUDED.professional, price = EXCLUDED.price, note = EXCLUDED.note, categoria = EXCLUDED.categoria, estat = 'Pendent';

-- 5. Sincronització de publicacions
INSERT INTO publicacions (id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost) VALUES 
('O-101', 'u-pro-fontanals', 'Fontanals Lampisteria & Climatització S.L.', 'OFERTA', 'Servei de lampisteria: Fontanals Lampisteria & Climatització S.L.', 'Reparació urgent de fuites d''aigua, desbussament d''aigüeres i calderes.', 'lampisteria', '45€ / hora'),
('O-102', 'u-pro-electromartinez', 'ElectroMartínez Instal·lacions Elèctriques S.L.', 'OFERTA', 'Servei d''electricitat: ElectroMartínez Instal·lacions Elèctriques S.L.', 'Diagnòstic de curtcircuits i quadres elèctrics.', 'electricitat', '60€'),
('O-103', 'u-pro-solerpintures', 'Soler Pintura & Alta Decoració S.L.', 'OFERTA', 'Servei de pintura: Soler Pintura & Alta Decoració S.L.', 'Pintura completa de saló o habitació amb pintura rentada.', 'pintura', '350€'),
('O-104', 'u-pro-fusteriaribes', 'Fusteria Artesana Ribes & Fills S.L.', 'OFERTA', 'Servei de fusteria: Fusteria Artesana Ribes & Fills S.L.', 'Ajustament de portes i panys de seguretat.', 'fusteria', '120€'),
('O-105', 'u-pro-climabeneyto', 'ClimaBeneyto Climatització & Aerotèrmia S.L.', 'OFERTA', 'Servei de climatització: ClimaBeneyto Climatització & Aerotèrmia S.L.', 'Manteniment i càrrega de gas aire condicionat.', 'climatitzacio', '85€'),
('O-106', 'u-pro-llevant', 'Llevant Reformes & Obres Integrals S.L.', 'OFERTA', 'Servei de reformes: Llevant Reformes & Obres Integrals S.L.', 'Treballs d''obra menor i enrajolat.', 'reformes', '95€')
ON CONFLICT (id) DO UPDATE 
SET usuari_id = EXCLUDED.usuari_id, usuari_nom = EXCLUDED.usuari_nom, tipus = EXCLUDED.tipus, titol = EXCLUDED.titol, descripcio = EXCLUDED.descripcio, categoria = EXCLUDED.categoria, preu_o_pressupost = EXCLUDED.preu_o_pressupost;

-- 6. INSERT de tasques finalitzades d'exemple
INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, budget, estat, tipus, valoracio, opinio) VALUES 
('T-101', 'Substitució de termo elèctric de 80L', 'Desmuntatge de l''aparell vell defectuós i connexió d''un nou termo amb vàlvula de seguretat.', 'lampisteria', 'u-cli-joan', 'Joan Navarro', 'u-pro-fontanals', 'Fontanals Lampisteria & Climatització S.L.', '180€', 'Finalitzada', 'tasca', 5, 'L''equip de Fontanals va ser puntualíssim i molt polit. Van desmuntar el termo vell i van deixar el nou funcionant impecable en dues hores.'),
('T-102', 'Instal·lació de línia independent per a inducció', 'Tirada de cable de 6mm des del quadre fins a la cuina amb magnetotèrmic dedicat de 25A.', 'electricitat', 'u-cli-marta', 'Marta Albiol', 'u-pro-electromartinez', 'ElectroMartínez Instal·lacions Elèctriques S.L.', '95€', 'Finalitzada', 'tasca', 5, 'Treball impecable! ElectroMartínez és una gran empresa, ens van explicar tot el quadre i van deixar la cuina perfecta.'),
('T-103', 'Pintura del dormitori principal i sostre', 'Dues capes de pintura plàstica color blanc trencat i tractament preventiu antihumitat.', 'pintura', 'u-cli-pau', 'Pau Ferrandis', 'u-pro-solerpintures', 'Soler Pintura & Alta Decoració S.L.', '220€', 'Finalitzada', 'tasca', 4, 'Molt bon acabat i pintura d''alta qualitat. Van protegir el terra a la perfecció i van acabar abans del previst.'),
('T-104', 'Manteniment i càrrega de gas aire condicionat', 'Revisió de vàlvules, substitució de filtres i càrrega ecològica R32 a split domèstic.', 'climatitzacio', 'u-cli-joan', 'Joan Navarro', 'u-pro-climabeneyto', 'ClimaBeneyto Climatització & Aerotèrmia S.L.', '85€', 'Finalitzada', 'tasca', 5, 'Molt ràpids i eficients. Ara l''equip refreda de meravella i fa molt menys soroll que abans.')
ON CONFLICT (id) DO UPDATE 
SET professional_id = EXCLUDED.professional_id, professional_nom = EXCLUDED.professional_nom, valoracio = EXCLUDED.valoracio, opinio = EXCLUDED.opinio;

-- 7. INSERT de ressenyes públiques per a les noves empreses
INSERT INTO resenyes (id, client_id, client_nom, treballador_id, professional_nom, tasca_id, puntuacio, descripcio) VALUES 
('RES-101', 'u-cli-joan', 'Joan Navarro', 'u-pro-fontanals', 'Fontanals Lampisteria & Climatització S.L.', 'T-101', 5, 'L''equip de Fontanals va ser puntualíssim i molt polit. Van desmuntar el termo vell i van deixar el nou funcionant impecable en dues hores.'),
('RES-102', 'u-cli-marta', 'Marta Albiol', 'u-pro-electromartinez', 'ElectroMartínez Instal·lacions Elèctriques S.L.', 'T-102', 5, 'Treball impecable! ElectroMartínez és una gran empresa, ens van explicar tot el quadre i van deixar la cuina perfecta.'),
('RES-103', 'u-cli-pau', 'Pau Ferrandis', 'u-pro-solerpintures', 'Soler Pintura & Alta Decoració S.L.', 'T-103', 4, 'Molt bon acabat i pintura d''alta qualitat. Van protegir el terra a la perfecció i van acabar abans del previst.'),
('RES-104', 'u-cli-joan', 'Joan Navarro', 'u-pro-climabeneyto', 'ClimaBeneyto Climatització & Aerotèrmia S.L.', 'T-104', 5, 'Molt ràpids i eficients. Ara l''equip refreda de meravella i fa molt menys soroll que abans.')
ON CONFLICT (id) DO UPDATE 
SET treballador_id = EXCLUDED.treballador_id, professional_nom = EXCLUDED.professional_nom, puntuacio = EXCLUDED.puntuacio, descripcio = EXCLUDED.descripcio;
