CREATE OR REPLACE FUNCTION align_column_to_date(p_table_name TEXT, p_column_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_udt_name TEXT;
BEGIN
    SELECT c.udt_name
    INTO v_udt_name
    FROM information_schema.columns c
    WHERE c.table_schema = current_schema()
      AND c.table_name = p_table_name
      AND c.column_name = p_column_name;

    IF v_udt_name IS NULL OR v_udt_name = 'date' THEN
        RETURN;
    END IF;

    IF v_udt_name = 'bytea' THEN
        EXECUTE format('UPDATE %I SET %I = NULL', p_table_name, p_column_name);
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE DATE USING NULL::DATE',
            p_table_name, p_column_name
        );
        RETURN;
    END IF;

    IF v_udt_name IN ('timestamp', 'timestamptz') THEN
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE DATE USING %I::DATE',
            p_table_name, p_column_name, p_column_name
        );
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION align_column_to_timestamptz(p_table_name TEXT, p_column_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_udt_name TEXT;
BEGIN
    SELECT c.udt_name
    INTO v_udt_name
    FROM information_schema.columns c
    WHERE c.table_schema = current_schema()
      AND c.table_name = p_table_name
      AND c.column_name = p_column_name;

    IF v_udt_name IS NULL OR v_udt_name = 'timestamptz' THEN
        RETURN;
    END IF;

    IF v_udt_name = 'bytea' THEN
        EXECUTE format('UPDATE %I SET %I = NULL', p_table_name, p_column_name);
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE TIMESTAMP WITH TIME ZONE USING NULL::TIMESTAMPTZ',
            p_table_name, p_column_name
        );
        RETURN;
    END IF;

    IF v_udt_name = 'timestamp' THEN
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE TIMESTAMP WITH TIME ZONE USING %I AT TIME ZONE ''UTC''',
            p_table_name, p_column_name, p_column_name
        );
    END IF;
END;
$$;

SELECT align_column_to_date('document', 'issue_date');
SELECT align_column_to_date('document', 'expiry_date');
SELECT align_column_to_date('kyc_document', 'issue_date');
SELECT align_column_to_date('kyc_document', 'expiry_date');

SELECT align_column_to_timestamptz('document', 'uploaded_at');
SELECT align_column_to_timestamptz('document', 'updated_at');
SELECT align_column_to_timestamptz('document_version', 'uploaded_at');
SELECT align_column_to_timestamptz('document_review', 'reviewed_at');
SELECT align_column_to_timestamptz('document_signature', 'signed_at');
SELECT align_column_to_timestamptz('kyc_case', 'started_at');
SELECT align_column_to_timestamptz('kyc_case', 'submitted_at');
SELECT align_column_to_timestamptz('kyc_case', 'completed_at');
SELECT align_column_to_timestamptz('kyc_case', 'reviewed_at');
SELECT align_column_to_timestamptz('kyc_verification', 'verified_at');

DROP FUNCTION align_column_to_date(TEXT, TEXT);
DROP FUNCTION align_column_to_timestamptz(TEXT, TEXT);
