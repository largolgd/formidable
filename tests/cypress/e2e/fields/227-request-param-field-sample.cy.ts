import {enableModule} from '@jahia/cypress';
import {createPublishedLiveFormPage, FORMIDABLE_TEST_SITE, visitLiveForm} from '../../support/fixtures';
import type {JahiaNode} from '../../support/fixtures';
import {useFormidableSite} from './support';

/** The sample module adding a hidden field fed by a request parameter of the page. */
const SAMPLE_MODULE = 'formidable-test-module-samples-tsx';
const FIELD = {name: 'campaign', parameter: 'utm_source', defaultValue: 'direct'};

const getRequestParamNode = (data: {name: string; parameter: string; defaultValue?: string}): JahiaNode => ({
	name: data.name,
	primaryNodeType: 'fmdbsampletsx:inputRequestParam',
	properties: [
		{name: 'requestParameter', value: data.parameter},
		...(data.defaultValue === undefined ? [] : [{name: 'defaultValue', value: data.defaultValue}])
	]
});

/** The value the page renders in the hidden field, read from the HTML a GET or a POST returns. */
const valueIn = (html: string): string | undefined =>
	new RegExp(`<input[^>]*name="${FIELD.name}"[^>]*>`).exec(html)?.[0].match(/value="([^"]*)"/)?.[1];

/**
 * A third-party module adds a hidden field type whose value is read from a request parameter of
 * the page — a GET parameter of the URL or a field of a POST to the page — the parameter's name
 * being the contributor's setting, falling back on a default value.
 */
describe('Form fields - 227 Request parameter field (third-party sample)', () => {
	useFormidableSite();

	before(() => {
		enableModule(SAMPLE_MODULE, FORMIDABLE_TEST_SITE.key);
	});

	it('submits the value of the GET parameter, or the default value when it is absent', () => {
		createPublishedLiveFormPage('request-param-form', 'Request Param Form', [
			getRequestParamNode(FIELD)
		]).then(({livePath}) => {
			const input = () => cy.get(`input[type="hidden"][name="${FIELD.name}"]`);

			// No parameter: the default value.
			visitLiveForm(livePath);
			input().should('have.value', FIELD.defaultValue);

			// The parameter replaces it...
			visitLiveForm(livePath, 'en', `${FIELD.parameter}=newsletter`);
			input().should('have.value', 'newsletter');

			// ...for that request only: the page cached for one value is not served for another.
			visitLiveForm(livePath);
			input().should('have.value', FIELD.defaultValue);
			visitLiveForm(livePath, 'en', `${FIELD.parameter}=social`);
			input().should('have.value', 'social');

			// An empty parameter, or another one, leaves the default.
			visitLiveForm(livePath, 'en', `${FIELD.parameter}=`);
			input().should('have.value', FIELD.defaultValue);
			visitLiveForm(livePath, 'en', 'other=newsletter');
			input().should('have.value', FIELD.defaultValue);
		});
	});

	it('takes the value from a POST to the page', () => {
		createPublishedLiveFormPage('request-param-post-form', 'Request Param Post Form', [
			getRequestParamNode(FIELD)
		]).then(({livePath}) => {
			const url = `/en/sites/${FORMIDABLE_TEST_SITE.key}/${livePath}`;

			cy.request({method: 'POST', url, form: true, body: {[FIELD.parameter]: 'from-post'}, failOnStatusCode: false})
				.then(response => {
					expect(response.status, 'status of the POST').to.equal(200);
					expect(valueIn(response.body as string), 'value rendered after the POST').to.equal('from-post');
				});

			// A POST leaves nothing behind: the next GET gets the default.
			cy.request(url).then(response => {
				expect(valueIn(response.body as string), 'value rendered after a plain GET').to.equal(FIELD.defaultValue);
			});
		});
	});
});
