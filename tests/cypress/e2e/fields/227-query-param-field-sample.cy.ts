import {enableModule} from '@jahia/cypress';
import {createPublishedLiveFormPage, FORMIDABLE_TEST_SITE, visitLiveForm} from '../../support/fixtures';
import type {JahiaNode} from '../../support/fixtures';
import {useFormidableSite} from './support';

/** The sample module adding a hidden field fed by a GET parameter of the page URL. */
const SAMPLE_MODULE = 'formidable-test-module-samples-tsx';
const FIELD = {name: 'campaign', parameter: 'utm_source', defaultValue: 'direct'};

const getQueryParamNode = (data: {name: string; parameter: string; defaultValue?: string}): JahiaNode => ({
	name: data.name,
	primaryNodeType: 'fmdbsampletsx:inputQueryParam',
	properties: [
		{name: 'queryParameter', value: data.parameter},
		...(data.defaultValue === undefined ? [] : [{name: 'defaultValue', value: data.defaultValue}])
	]
});

/**
 * A third-party module adds a hidden field type whose value is read from a GET parameter of the
 * page URL — the parameter's name being the contributor's setting — falling back on a default value.
 */
describe('Form fields - 227 Query parameter field (third-party sample)', () => {
	useFormidableSite();

	before(() => {
		enableModule(SAMPLE_MODULE, FORMIDABLE_TEST_SITE.key);
	});

	it('submits the value of the URL parameter, or the default value when it is absent', () => {
		createPublishedLiveFormPage('query-param-form', 'Query Param Form', [
			getQueryParamNode(FIELD)
		]).then(({livePath}) => {
			const input = () => cy.get(`input[type="hidden"][name="${FIELD.name}"]`);

			// No parameter: the default value.
			visitLiveForm(livePath);
			input().should('have.value', FIELD.defaultValue);

			// The parameter replaces it once the island is hydrated.
			visitLiveForm(livePath, 'en', `${FIELD.parameter}=newsletter`);
			input().should('have.value', 'newsletter');

			// An empty parameter, or another one, leaves the default.
			visitLiveForm(livePath, 'en', `${FIELD.parameter}=`);
			input().should('have.value', FIELD.defaultValue);
			visitLiveForm(livePath, 'en', 'other=newsletter');
			input().should('have.value', FIELD.defaultValue);
		});
	});
});
