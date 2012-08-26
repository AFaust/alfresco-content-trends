/*
 * Need a dummy field, since picker.inc.ftl (included by our template) makes an assignment outside of a macro scope that only is required
 * for the macro we DO NOT need, but which requires access to a single field parameter => I consider this a coding bug
 */
model.field =
{
    control :
    {
        params : {
            compactMode: true
        }
    }
};