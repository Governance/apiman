/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.manager.ui.client.local.pages.app;

import io.apiman.manager.api.beans.summary.ContractSummaryBean;
import io.apiman.manager.ui.client.local.AppMessages;
import io.apiman.manager.ui.client.local.events.BreakContractEvent;
import io.apiman.manager.ui.client.local.events.BreakContractEvent.Handler;
import io.apiman.manager.ui.client.local.events.BreakContractEvent.HasBreakContractHandlers;
import io.apiman.manager.ui.client.local.events.ConfirmationEvent;
import io.apiman.manager.ui.client.local.pages.ConsumerOrgPage;
import io.apiman.manager.ui.client.local.pages.ConsumerServicePage;
import io.apiman.manager.ui.client.local.pages.common.NoEntitiesWidget;
import io.apiman.manager.ui.client.local.services.NavigationHelperService;
import io.apiman.manager.ui.client.local.util.Formatting;
import io.apiman.manager.ui.client.local.util.MultimapUtil;
import io.apiman.manager.ui.client.local.widgets.ConfirmationDialog;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.overlord.commons.gwt.client.local.widgets.AsyncActionButton;
import org.overlord.commons.gwt.client.local.widgets.FontAwesomeIcon;
import org.overlord.commons.gwt.client.local.widgets.SpanPanel;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * Models a list of contracts on the Application / Contract page/tab.
 *
 * @author eric.wittmann@redhat.com
 */
@Dependent
public class AppContractList extends FlowPanel implements HasValue<List<ContractSummaryBean>>, HasBreakContractHandlers {
    
    @Inject
    protected NavigationHelperService navHelper;
    @Inject
    protected TranslationService i18n;
    @Inject
    Instance<ConfirmationDialog> confirmationDialogFactory;
    
    private List<ContractSummaryBean> contracts;
    private boolean filtered;
    
    /**
     * Constructor.
     */
    public AppContractList() {
        getElement().setClassName("apiman-contracts"); //$NON-NLS-1$
    }

    /**
     * @see com.google.gwt.event.logical.shared.HasValueChangeHandlers#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<List<ContractSummaryBean>> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    /**
     * @see io.apiman.manager.ui.client.local.events.BreakContractEvent.HasBreakContractHandlers#addBreakContractHandler(io.apiman.manager.ui.client.local.events.BreakContractEvent.Handler)
     */
    @Override
    public HandlerRegistration addBreakContractHandler(Handler handler) {
        return addHandler(handler, BreakContractEvent.getType());
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#getValue()
     */
    @Override
    public List<ContractSummaryBean> getValue() {
        return contracts;
    }
    
    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    public void setFilteredValue(List<ContractSummaryBean> value) {
        filtered = true;
        setValue(value, false);
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    @Override
    public void setValue(List<ContractSummaryBean> value) {
        filtered = false;
        setValue(value, false);
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
     */
    @Override
    public void setValue(List<ContractSummaryBean> value, boolean fireEvents) {
        contracts = value;
        clear();
        refresh();
    }

    /**
     * Refresh the display with the current value.
     */
    public void refresh() {
        if (contracts != null && !contracts.isEmpty()) {
            for (ContractSummaryBean bean : contracts) {
                Widget row = createContractRow(bean);
                add(row);
            }
        } else {
            add(createNoEntitiesWidget());
        }
    }


    /**
     * @return a widget to show when there are no entities.
     */
    protected NoEntitiesWidget createNoEntitiesWidget() {
        if (isFiltered())
            return new NoEntitiesWidget(i18n.format(AppMessages.NO_FILTERED_CONTRACTS_FOR_APP_MESSAGE), true);
        else
            return new NoEntitiesWidget(i18n.format(AppMessages.NO_CONTRACTS_FOR_APP_MESSAGE), true);
    }

    /**
     * Creates a single contract row.
     * @param bean
     */
    private Widget createContractRow(ContractSummaryBean bean) {
        FlowPanel container = new FlowPanel();
        container.getElement().setClassName("container-fluid"); //$NON-NLS-1$
        container.getElement().addClassName("apiman-summaryrow"); //$NON-NLS-1$
        
        FlowPanel row = new FlowPanel();
        container.add(row);
        row.getElement().setClassName("row"); //$NON-NLS-1$
        
        createSummaryColumn(bean, row);

        createActionColumn(bean, row);
        
        container.add(new HTMLPanel("<hr/>")); //$NON-NLS-1$
        
        return container;
    }

    /**
     * Creates the summary column.
     * @param bean
     * @param row
     */
    protected void createSummaryColumn(final ContractSummaryBean bean, FlowPanel row) {
        FlowPanel col = new FlowPanel();
        row.add(col);
        col.setStyleName("col-md-10"); //$NON-NLS-1$
        col.addStyleName("col-no-padding"); //$NON-NLS-1$
        
        Anchor org = new Anchor(bean.getServiceOrganizationName());
        col.add(org);
        org.setHref(navHelper.createHrefToPage(ConsumerOrgPage.class, MultimapUtil.fromMultiple("org", bean.getServiceOrganizationId()))); //$NON-NLS-1$
        InlineLabel divider = new InlineLabel(" / "); //$NON-NLS-1$
        col.add(divider);
        SpanPanel sp = new SpanPanel();
        col.add(sp);
        sp.getElement().setClassName("title"); //$NON-NLS-1$
        Anchor a = new Anchor(bean.getServiceName());
        sp.add(a);
        a.setHref(navHelper.createHrefToPage(ConsumerServicePage.class,
                MultimapUtil.fromMultiple("org", bean.getServiceOrganizationId(), "service", bean.getServiceId()))); //$NON-NLS-1$ //$NON-NLS-2$

        FlowPanel versionAndPlan = new FlowPanel();
        col.add(versionAndPlan);
        versionAndPlan.setStyleName("versionAndPlan"); //$NON-NLS-1$
        versionAndPlan.add(new InlineLabel(i18n.format(AppMessages.SERVICE_VERSION) + " ")); //$NON-NLS-1$
        sp = new SpanPanel();
        versionAndPlan.add(sp);
        a = new Anchor(bean.getServiceVersion());
        sp.add(a);
        a.setHref(navHelper.createHrefToPage(ConsumerServicePage.class,
                MultimapUtil.fromMultiple("org", bean.getServiceOrganizationId(), "service", bean.getServiceId(), "version", bean.getServiceVersion()))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        versionAndPlan.add(new InlineLabel(" " + i18n.format(AppMessages.VIA_PLAN) + " ")); //$NON-NLS-1$ //$NON-NLS-2$
        sp = new SpanPanel();
        versionAndPlan.add(sp);
        a = new Anchor(bean.getPlanName());
        sp.add(a);
//        a.setHref(navHelper.createHrefToPage(PlanOverviewPage.class,
//                MultimapUtil.fromMultiple("org", bean.getServiceOrganizationId(), "plan", bean.getPlanId(), "version", bean.getPlanVersion()))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        versionAndPlan.add(new InlineLabel(" " + i18n.format(AppMessages.ENTERED_INTO_ON) + " ")); //$NON-NLS-1$ //$NON-NLS-2$
        FontAwesomeIcon icon = new FontAwesomeIcon("clock-o", true); //$NON-NLS-1$
        versionAndPlan.add(icon);
        icon.getElement().addClassName("fa-inline"); //$NON-NLS-1$
        versionAndPlan.add(new InlineLabel(Formatting.formatShortDate(bean.getCreatedOn())));
        
        Label description = new Label(bean.getServiceDescription());
        col.add(description);
        description.getElement().setClassName("description"); //$NON-NLS-1$
        description.getElement().addClassName("apiman-label-faded"); //$NON-NLS-1$
    }
    
    /**
     * Creates the action column for the single contract row.
     * @param bean
     * @param row
     */
    protected void createActionColumn(final ContractSummaryBean bean, FlowPanel row) {
        FlowPanel col = new FlowPanel();
        row.add(col);
        col.setStyleName("col-md-2"); //$NON-NLS-1$
        col.addStyleName("col-no-padding"); //$NON-NLS-1$
        
        SpanPanel sp = new SpanPanel();
        col.add(sp);
        sp.getElement().setClassName("actions"); //$NON-NLS-1$
        final AsyncActionButton aab = new AsyncActionButton();
        aab.getElement().setClassName("btn"); //$NON-NLS-1$
        aab.getElement().addClassName("btn-default"); //$NON-NLS-1$
        aab.getElement().setAttribute("data-permission", "appEdit"); //$NON-NLS-1$ //$NON-NLS-2$
        aab.getElement().setAttribute("data-status", "Created,Ready"); //$NON-NLS-1$ //$NON-NLS-2$
        aab.setHTML(i18n.format(AppMessages.BREAK_CONTRACT));
        aab.setActionText(i18n.format(AppMessages.BREAKING));
        aab.setIcon("fa-cog"); //$NON-NLS-1$
        aab.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                aab.onActionStarted();
                aab.getElement().getStyle().setVisibility(Visibility.VISIBLE);
                ConfirmationDialog dialog = confirmationDialogFactory.get();
                dialog.setDialogTitle(i18n.format(AppMessages.CONFIRM_BREAK_CONTRACT_TITLE));
                dialog.setDialogMessage(i18n.format(AppMessages.CONFIRM_BREAK_CONTRACT_MESSAGE, bean.getServiceName()));
                dialog.addConfirmationHandler(new ConfirmationEvent.Handler() {
                    @Override
                    public void onConfirmation(ConfirmationEvent event) {
                        if (event.isConfirmed()) {
                            BreakContractEvent.fire(AppContractList.this, bean);
                        } else {
                            aab.onActionComplete();
                            aab.getElement().getStyle().clearVisibility();
                        }
                    }
                });
                dialog.show();
            }
        });
        sp.add(aab);
    }

    /**
     * @return the filtered
     */
    protected boolean isFiltered() {
        return filtered;
    }

}
