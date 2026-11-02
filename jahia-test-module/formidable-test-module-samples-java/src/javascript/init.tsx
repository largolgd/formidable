import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';
import {ContactRoutingCmp} from './ContactRouting/ContactRoutingCmp';

export default function () {
    registry.add('callback', 'FormidableSamplesJavaEditor', {
        targets: ['jahiaApp-init:20'],
        callback: () => {
            i18next.loadNamespaces('formidable-test-module-samples-java');

            // Picked by the editor form of fmdbsample:emailContentToContactAction (META-INF/jahia-content-editor-forms)
            registry.add('selectorType', 'ContactRouting', {cmp: ContactRoutingCmp, supportMultiple: false});
        }
    });
}
