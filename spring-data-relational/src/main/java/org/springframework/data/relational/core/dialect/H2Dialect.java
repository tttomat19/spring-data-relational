/*
 * Copyright 2019-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.relational.core.dialect;

import java.util.Collections;
import java.util.Set;

import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.IdentifierProcessing.LetterCasing;
import org.springframework.data.relational.core.sql.IdentifierProcessing.Quoting;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An SQL dialect for H2.
 *
 * @author Mark Paluch
 * @author Myeonghyeon Lee
 * @author Christph Strobl
 * @author Jens Schauder
 * @author Mikhail Polivakha
 * @since 2.0
 */
public class H2Dialect extends AbstractDialect {

	/**
	 * Singleton instance.
	 *
	 * @deprecated use either the {@code org.springframework.data.r2dbc.dialect.H2Dialect} or
	 * 						 {@code org.springframework.data.jdbc.core.dialect.JdbcH2Dialect}.
	 */
	@Deprecated(forRemoval = true)
	public static final H2Dialect INSTANCE = new H2Dialect();

	private static final IdentifierProcessing IDENTIFIER_PROCESSING = IdentifierProcessing.create(Quoting.ANSI,
			LetterCasing.UPPER_CASE);
	private static final IdGeneration ID_GENERATION = IdGeneration.create(IDENTIFIER_PROCESSING);

	private static final LimitClause LIMIT_CLAUSE = new LimitClause() {

		@Override
		public String getLimit(long limit) {
			return "LIMIT " + limit;
		}

		@Override
		public String getOffset(long offset) {
			return "OFFSET " + offset;
		}

		@Override
		public String getLimitOffset(long limit, long offset) {
			return String.format("OFFSET %d ROWS FETCH FIRST %d ROWS ONLY", offset, limit);
		}

		@Override
		public Position getClausePosition() {
			return Position.AFTER_ORDER_BY;
		}
	};

	protected H2Dialect() {}

	@Override
	public LimitClause limit() {
		return LIMIT_CLAUSE;
	}

	@Override
	public LockClause lock() {
		return AnsiDialect.LOCK_CLAUSE;
	}

	@Override
	public ArrayColumns getArraySupport() {
		return H2ArrayColumns.INSTANCE;
	}

	protected static class H2ArrayColumns implements ArrayColumns {

		public static final H2ArrayColumns INSTANCE = new H2ArrayColumns();

		@Override
		public boolean isSupported() {
			return true;
		}

		@Override
		public Class<?> getArrayType(Class<?> userType) {

			Assert.notNull(userType, "Array component type must not be null");

			return ClassUtils.resolvePrimitiveIfNecessary(userType);
		}
	}

	@Override
	public IdentifierProcessing getIdentifierProcessing() {
		return IDENTIFIER_PROCESSING;
	}

	@Override
	public Set<Class<?>> simpleTypes() {

		return Collections.emptySet();
	}

	@Override
	public boolean supportsSingleQueryLoading() {
		return false;
	}

	@Override
	public IdGeneration getIdGeneration() {
		return ID_GENERATION;
	}
}
