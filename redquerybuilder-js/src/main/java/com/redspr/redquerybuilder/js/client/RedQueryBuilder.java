package com.redspr.redquerybuilder.js.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsNamespace;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.redspr.redquerybuilder.core.client.Configuration;
import com.redspr.redquerybuilder.core.client.command.CommandBuilder;
import com.redspr.redquerybuilder.core.client.command.dml.Select;
import com.redspr.redquerybuilder.core.client.engine.Session;
import com.redspr.redquerybuilder.core.client.engine.TableEvent;
import com.redspr.redquerybuilder.core.client.engine.TableEventHandler;
import com.redspr.redquerybuilder.core.client.expression.ExpressionColumn;
import com.redspr.redquerybuilder.core.client.table.TableFilter;
import com.redspr.redquerybuilder.core.client.util.ObjectArray;
import com.redspr.redquerybuilder.core.shared.meta.Column;

/**
 * Bootstraps the JS version.
 *
 * NB methods in order or lifecycle rather than normal rules.
 */
@JsNamespace("$wnd.rqb")
@com.google.gwt.core.client.js.JsType
public class RedQueryBuilder implements EntryPoint {

    private final SimplePanel builderContainer = new SimplePanel();


    @Override
    public void onModuleLoad() {

    }

// TODO 00 unit test, null args
    @JsExport("$wnd.redQueryBuilder")
    static CommandBuilder configure(JsConfiguration config, String sql,
            JsArrayMixed args) throws Exception {
        if (config == null) {
            throw new IllegalArgumentException("Config is null.");
        }

        RedQueryBuilder rqb = new RedQueryBuilder();

        return rqb.install(config, sql, JsList.get().toList(args));
    }

    private CommandBuilder install(JsConfiguration config2, String sql, List<Object> args)
            throws Exception {
        Configuration config = new ConfigurationAdapter(config2);

        RootPanel.get(config2.getTargetId()).add(builderContainer);
        return createCommandBuilder(config, sql, args);
    }

    private CommandBuilder createCommandBuilder(final Configuration config2, String sql,
            List<Object> args) throws Exception {
        final Session session = new Session(config2);

        final CommandBuilder builder = new CommandBuilder(session, sql, args);

        session.getMsgBus().addHandler(TableEvent.TYPE,
                new TableEventHandler() {
                    @Override
                    public void onTable(TableEvent e) {
                        ObjectArray expr;
                        if (session.getFilters().size() > 0) {
                            config2.fireOnTableChange(session.getFilters());
                            // XXX need to do distinct?
                            expr = ObjectArray.newInstance();
                            TableFilter tf = session.getFilters().get(0);
                            String alias = tf.getAlias();
                            for (Column col : tf.getTable().getColumns()) {
                                expr.add(new ExpressionColumn(session, null, alias,
                                    col.getName()));
                            }
                        } else {
                            expr = null;
                        }
                        builder.getSelect().setExpressions(expr);
                    }
                });

        builder.addValueChangeHandler(new ValueChangeHandler<Select>() {
            @Override
            public void onValueChange(ValueChangeEvent<Select> event) {
                List<Object> args = new ArrayList<Object>();
                String sql = event.getValue().getSQL(args);

                config2.fireOnSqlChange(sql, args);
            }
        });

        builderContainer.setWidget(builder);

        config2.fireOnTableChange(session.getFilters());

        return builder;
    }
}
