DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'document'
          AND column_name = 'uploaded_at'
          AND udt_name = 'bytea'
    ) THEN
        EXECUTE 'UPDATE document SET uploaded_at = NULL';
        EXECUTE 'ALTER TABLE document ALTER COLUMN uploaded_at TYPE TIMESTAMP WITH TIME ZONE USING NULL::TIMESTAMPTZ';
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'document'
          AND column_name = 'uploaded_at'
          AND udt_name = 'timestamp'
    ) THEN
        EXECUTE 'ALTER TABLE document ALTER COLUMN uploaded_at TYPE TIMESTAMP WITH TIME ZONE USING uploaded_at AT TIME ZONE ''UTC''';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'document'
          AND column_name = 'updated_at'
          AND udt_name = 'bytea'
    ) THEN
        EXECUTE 'UPDATE document SET updated_at = NULL';
        EXECUTE 'ALTER TABLE document ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING NULL::TIMESTAMPTZ';
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'document'
          AND column_name = 'updated_at'
          AND udt_name = 'timestamp'
    ) THEN
        EXECUTE 'ALTER TABLE document ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE ''UTC''';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'document_version'
          AND column_name = 'uploaded_at'
          AND udt_name = 'timestamp'
    ) THEN
        EXECUTE 'ALTER TABLE document_version ALTER COLUMN uploaded_at TYPE TIMESTAMP WITH TIME ZONE USING uploaded_at AT TIME ZONE ''UTC''';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'document_review'
          AND column_name = 'reviewed_at'
          AND udt_name = 'timestamp'
    ) THEN
        EXECUTE 'ALTER TABLE document_review ALTER COLUMN reviewed_at TYPE TIMESTAMP WITH TIME ZONE USING reviewed_at AT TIME ZONE ''UTC''';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'document_signature'
          AND column_name = 'signed_at'
          AND udt_name = 'timestamp'
    ) THEN
        EXECUTE 'ALTER TABLE document_signature ALTER COLUMN signed_at TYPE TIMESTAMP WITH TIME ZONE USING signed_at AT TIME ZONE ''UTC''';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'kyc_case'
          AND column_name IN ('started_at', 'submitted_at', 'completed_at', 'reviewed_at')
          AND udt_name = 'timestamp'
    ) THEN
        EXECUTE 'ALTER TABLE kyc_case ALTER COLUMN started_at TYPE TIMESTAMP WITH TIME ZONE USING started_at AT TIME ZONE ''UTC''';
        EXECUTE 'ALTER TABLE kyc_case ALTER COLUMN submitted_at TYPE TIMESTAMP WITH TIME ZONE USING submitted_at AT TIME ZONE ''UTC''';
        EXECUTE 'ALTER TABLE kyc_case ALTER COLUMN completed_at TYPE TIMESTAMP WITH TIME ZONE USING completed_at AT TIME ZONE ''UTC''';
        EXECUTE 'ALTER TABLE kyc_case ALTER COLUMN reviewed_at TYPE TIMESTAMP WITH TIME ZONE USING reviewed_at AT TIME ZONE ''UTC''';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'kyc_verification'
          AND column_name = 'verified_at'
          AND udt_name = 'timestamp'
    ) THEN
        EXECUTE 'ALTER TABLE kyc_verification ALTER COLUMN verified_at TYPE TIMESTAMP WITH TIME ZONE USING verified_at AT TIME ZONE ''UTC''';
    END IF;
END $$;
