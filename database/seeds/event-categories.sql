-- Owner: Đông / DONG-02. Input must come from Vương's fixture/category registry.
-- On the same connection, set TC_EVENT_CATEGORY_REGISTRY to a JSON array of
-- {id: canonical UUID, code: string, name: string, active: boolean, displayOrder: integer}.
-- No generated IDs and no default category data are substituted for a missing registry.
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @registry nvarchar(max) = TRY_CONVERT(nvarchar(max), SESSION_CONTEXT(N'TC_EVENT_CATEGORY_REGISTRY'));
IF @registry IS NULL OR ISJSON(@registry) <> 1 OR LEFT(LTRIM(@registry), 1) <> N'['
    THROW 51021, 'Category seed requires the approved TC_EVENT_CATEGORY_REGISTRY JSON array on this connection.', 1;
IF NOT EXISTS (SELECT 1 FROM OPENJSON(@registry))
    THROW 51021, 'Category registry cannot be empty.', 1;
IF EXISTS (SELECT 1 FROM OPENJSON(@registry) entries WHERE entries.type <> 5)
    THROW 51021, 'Every category registry entry must be a JSON object.', 1;
IF EXISTS (
    SELECT 1 FROM OPENJSON(@registry) entries
    WHERE TRY_CONVERT(uniqueidentifier, JSON_VALUE(entries.value, '$.id')) IS NULL
       OR DATALENGTH(JSON_VALUE(entries.value, '$.id')) <> 72
       OR JSON_VALUE(entries.value, '$.code') IS NULL
       OR DATALENGTH(JSON_VALUE(entries.value, '$.code')) NOT BETWEEN 2 AND 128
       OR LEN(LTRIM(RTRIM(JSON_VALUE(entries.value, '$.code')))) = 0
       OR JSON_VALUE(entries.value, '$.name') IS NULL
       OR DATALENGTH(JSON_VALUE(entries.value, '$.name')) NOT BETWEEN 2 AND 400
       OR LEN(LTRIM(RTRIM(JSON_VALUE(entries.value, '$.name')))) = 0
       OR JSON_VALUE(entries.value, '$.active') IS NULL
       OR JSON_VALUE(entries.value, '$.active') COLLATE Latin1_General_100_BIN2 NOT IN (N'true', N'false')
       OR TRY_CONVERT(int, JSON_VALUE(entries.value, '$.displayOrder')) IS NULL
       OR TRY_CONVERT(int, JSON_VALUE(entries.value, '$.displayOrder')) < 0)
    THROW 51021, 'Category registry contains an invalid row.', 1;

DECLARE @categories TABLE (
    id uniqueidentifier NOT NULL,
    code nvarchar(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    name nvarchar(200) NOT NULL,
    active bit NOT NULL,
    displayOrder int NOT NULL
);
INSERT @categories (id, code, name, active, displayOrder)
SELECT CONVERT(uniqueidentifier, JSON_VALUE(value, '$.id')), JSON_VALUE(value, '$.code'),
       JSON_VALUE(value, '$.name'), CASE JSON_VALUE(value, '$.active') WHEN N'true' THEN 1 ELSE 0 END,
       CONVERT(int, JSON_VALUE(value, '$.displayOrder'))
FROM OPENJSON(@registry);
IF EXISTS (SELECT code FROM @categories GROUP BY code HAVING COUNT(*) > 1)
   OR EXISTS (SELECT id FROM @categories GROUP BY id HAVING COUNT(*) > 1)
    THROW 51021, 'Category registry IDs and codes must each be unique.', 1;

DECLARE @ownsTransaction bit = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
IF @ownsTransaction = 1 BEGIN TRANSACTION;
ELSE SAVE TRANSACTION DongCategorySeed;
BEGIN TRY
    IF EXISTS (
        SELECT 1 FROM dbo.EventCategory currentCategory WITH (UPDLOCK, HOLDLOCK)
        JOIN @categories seed ON currentCategory.code = seed.code OR currentCategory.id = seed.id
        WHERE currentCategory.id <> seed.id OR currentCategory.code <> seed.code)
        THROW 51021, 'Category registry conflicts with an existing ID/code mapping.', 1;

    UPDATE currentCategory
    SET name = seed.name, active = seed.active, displayOrder = seed.displayOrder
    FROM dbo.EventCategory currentCategory WITH (UPDLOCK, HOLDLOCK)
    JOIN @categories seed ON currentCategory.code = seed.code
    WHERE currentCategory.name COLLATE Latin1_General_100_BIN2 <> seed.name COLLATE Latin1_General_100_BIN2
       OR currentCategory.active <> seed.active OR currentCategory.displayOrder <> seed.displayOrder;

    INSERT dbo.EventCategory (id, code, name, active, displayOrder)
    SELECT seed.id, seed.code, seed.name, seed.active, seed.displayOrder
    FROM @categories seed
    WHERE NOT EXISTS (SELECT 1 FROM dbo.EventCategory currentCategory WITH (UPDLOCK, HOLDLOCK)
                      WHERE currentCategory.code = seed.code);
    IF @ownsTransaction = 1 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @ownsTransaction = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    ELSE IF @ownsTransaction = 0 AND XACT_STATE() = 1 ROLLBACK TRANSACTION DongCategorySeed;
    THROW;
END CATCH;
