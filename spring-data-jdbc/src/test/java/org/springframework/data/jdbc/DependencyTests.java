/*
 * Copyright 2017-2025 the original author or authors.
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
package org.springframework.data.jdbc;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.config.AuditingHandlerBeanDefinitionParser;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Test package dependencies for violations.
 *
 * @author Jens Schauder
 */
@Disabled("Disabled because of JdbcArrayColumns and Dialect cycle to be resolved in 4.0")
public class DependencyTests {

	@Test
	void cycleFree() {

		JavaClasses importedClasses = new ClassFileImporter() //
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS) //
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS) // we just analyze the code of this module.
				.importPackages("org.springframework.data.jdbc").that( //
						onlySpringData() //
				);

		ArchRule rule = SlicesRuleDefinition.slices() //
				.matching("org.springframework.data.jdbc.(**)") //
				.should() //
				.beFreeOfCycles();

		rule.check(importedClasses);
	}

	@Test
	void acrossModules() {

		JavaClasses importedClasses = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages( //
						"org.springframework.data.jdbc", // Spring Data JDBC
						"org.springframework.data.relational", // Spring Data Relational
						"org.springframework.data" // Spring Data Commons
				).that(onlySpringData()) //
				.that(ignore(AuditingHandlerBeanDefinitionParser.class)) //
				.that(ignorePackage("org.springframework.data.aot.hint")) // ignoring aot, since it causes cycles in commons
				.that(ignorePackage("org.springframework.data.aot")); // ignoring aot, since it causes cycles in commons

		ArchRule rule = SlicesRuleDefinition.slices() //
				.assignedFrom(subModuleSlicing()) //
				.should().beFreeOfCycles();

		rule.check(importedClasses);
	}

	@Test // GH-1058
	void testGetFirstPackagePart() {

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(getFirstPackagePart("a.b.c")).isEqualTo("a");
			softly.assertThat(getFirstPackagePart("a")).isEqualTo("a");
		});
	}

	@Test // GH-1058
	void testSubModule() {

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(subModule("a.b", "a.b.c.d")).isEqualTo("c");
			softly.assertThat(subModule("a.b", "a.b.c")).isEqualTo("c");
			softly.assertThat(subModule("a.b", "a.b")).isEqualTo("");
		});
	}

	private DescribedPredicate<JavaClass> onlySpringData() {

		return new DescribedPredicate<>("Spring Data Classes") {
			@Override
			public boolean test(JavaClass input) {
				return input.getPackageName().startsWith("org.springframework.data");
			}
		};
	}

	private DescribedPredicate<JavaClass> ignore(Class<?> type) {

		return new DescribedPredicate<>("ignored class " + type.getName()) {
			@Override
			public boolean test(JavaClass input) {
				return !input.getFullName().startsWith(type.getName());
			}
		};
	}

	private DescribedPredicate<JavaClass> ignorePackage(String type) {

		return new DescribedPredicate<>("ignored class " + type) {
			@Override
			public boolean test(JavaClass input) {
				return !input.getPackageName().equals(type);
			}
		};
	}

	private String getFirstPackagePart(String subpackage) {

		int index = subpackage.indexOf(".");
		if (index < 0) {
			return subpackage;
		}
		return subpackage.substring(0, index);
	}

	private String subModule(String basePackage, String packageName) {

		if (packageName.startsWith(basePackage) && packageName.length() > basePackage.length()) {

			final int index = basePackage.length() + 1;
			String subpackage = packageName.substring(index);
			return getFirstPackagePart(subpackage);
		}
		return "";
	}

	private SliceAssignment subModuleSlicing() {
		return new SliceAssignment() {

			@Override
			public SliceIdentifier getIdentifierOf(JavaClass javaClass) {

				String packageName = javaClass.getPackageName();

				String subModule = subModule("org.springframework.data.jdbc", packageName);
				if (!subModule.isEmpty()) {
					return SliceIdentifier.of(subModule);
				}

				subModule = subModule("org.springframework.data.relational", packageName);
				if (!subModule.isEmpty()) {
					return SliceIdentifier.of(subModule);
				}

				subModule = subModule("org.springframework.data", packageName);
				if (!subModule.isEmpty()) {
					return SliceIdentifier.of(subModule);
				}

				return SliceIdentifier.ignore();
			}

			@Override
			public String getDescription() {
				return "Submodule";
			}
		};
	}

}
