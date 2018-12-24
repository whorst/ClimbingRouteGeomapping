-- Table: "RockRouteSchema"."RockRouteTable"

-- DROP TABLE "RockRouteSchema"."RockRouteTable";

CREATE TABLE "RockRouteSchema"."RockRouteTable"
(
    "RockRouteInfo" json NOT NULL
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE "RockRouteSchema"."RockRouteTable"
    OWNER to admin;