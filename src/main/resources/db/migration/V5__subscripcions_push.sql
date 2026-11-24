-- Subscripcions de notificacions push (Web Push) dels navegadors dels usuaris.
-- L'endpoint és únic: el mateix navegador només rep un cop, i si canvia de compte passa al nou usuari.
CREATE TABLE subscripcions_push (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuari_id UUID NOT NULL REFERENCES usuaris(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    p256dh TEXT NOT NULL,
    auth TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscripcions_push_endpoint_unic UNIQUE (endpoint),
    CONSTRAINT subscripcions_push_endpoint_https CHECK (endpoint ~ '^https://' AND LENGTH(endpoint) <= 2048)
);
CREATE INDEX subscripcions_push_usuari ON subscripcions_push (usuari_id);
