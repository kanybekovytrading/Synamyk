-- Fill Kyrgyz (name_ky) translations for all Kyrgyzstan regions.
-- Matches rows inserted in V1 by name to handle any id sequence.

UPDATE regions SET name_ky = 'Бишкек'            WHERE name = 'Бишкек';
UPDATE regions SET name_ky = 'Чүй облусу'         WHERE name = 'Чуйская область';
UPDATE regions SET name_ky = 'Ысык-Көл облусу'    WHERE name = 'Иссык-Кульская область';
UPDATE regions SET name_ky = 'Нарын облусу'        WHERE name = 'Нарынская область';
UPDATE regions SET name_ky = 'Талас облусу'        WHERE name = 'Таласская область';
UPDATE regions SET name_ky = 'Жалал-Абад облусу'  WHERE name = 'Джалал-Абадская область';
UPDATE regions SET name_ky = 'Ош облусу'           WHERE name = 'Ошская область';
UPDATE regions SET name_ky = 'Баткен облусу'       WHERE name = 'Баткенская область';
UPDATE regions SET name_ky = 'Ош'                  WHERE name = 'Ош';
