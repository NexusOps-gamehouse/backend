BEGIN;

UPDATE crew_svc.shop_items
SET price_hc = 200
WHERE code = 'BANNER_GUARDIAN_NAME'
  AND price_hc IS DISTINCT FROM 200;

COMMIT;
