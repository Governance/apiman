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
package io.apiman.manager.ui.client.local.pages;

import io.apiman.manager.api.beans.search.SearchCriteriaBean;
import io.apiman.manager.api.beans.search.SearchCriteriaFilterOperator;
import io.apiman.manager.api.beans.search.SearchResultsBean;
import io.apiman.manager.api.beans.summary.OrganizationSummaryBean;
import io.apiman.manager.ui.client.local.AppMessages;
import io.apiman.manager.ui.client.local.pages.common.Breadcrumb;
import io.apiman.manager.ui.client.local.pages.consumer.OrganizationList;
import io.apiman.manager.ui.client.local.services.ConfigurationService;
import io.apiman.manager.ui.client.local.services.rest.IRestInvokerCallback;
import io.apiman.manager.ui.client.local.util.MultimapUtil;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.PageState;
import org.jboss.errai.ui.nav.client.local.TransitionTo;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBox;


/**
 * The "Browse Organizations" page - part of the consumer UI.  This page
 * allows users to search for and find Organizations.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/consumer-orgs.html#page")
@Page(path="corgs")
@Dependent
public class ConsumerOrgsPage extends AbstractPage {
    
    @PageState
    protected String query;

    @Inject @DataField
    Breadcrumb breadcrumb;

    @Inject @DataField
    private TextBox searchBox;
    @Inject @DataField
    private Button searchButton;
    @Inject @DataField
    private OrganizationList orgs;
    
    @Inject
    ConfigurationService config;
    @Inject
    TransitionTo<ConsumerOrgsPage> toSelf;

    protected List<OrganizationSummaryBean> orgBeans;

    /**
     * Constructor.
     */
    public ConsumerOrgsPage() {
    }
    
    @PostConstruct
    protected void postConstruct() {
        searchBox.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                toSelf.go(MultimapUtil.singleItemMap("query", searchBox.getValue())); //$NON-NLS-1$
            }
        });
        searchButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                toSelf.go(MultimapUtil.singleItemMap("query", searchBox.getValue())); //$NON-NLS-1$
            }
        });
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#doLoadPageData()
     */
    @Override
    protected int doLoadPageData() {
        int rval = super.doLoadPageData();
        if (query != null && !query.trim().isEmpty()) {
            doQuery(query);
            rval += 1;
        } else {
            orgBeans = new ArrayList<OrganizationSummaryBean>();
        }
        return rval;
    }

    /**
     * Invoke the rest service to perform the query and get back the data.
     * @param query
     */
    private void doQuery(String query) {
        SearchCriteriaBean criteria = new SearchCriteriaBean();
        criteria.setPageSize(100);
        criteria.setPage(1);
        criteria.setOrder("name", true); //$NON-NLS-1$
        criteria.addFilter("name", "*" + query + "*", SearchCriteriaFilterOperator.like); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        rest.findOrganizations(criteria, new IRestInvokerCallback<SearchResultsBean<OrganizationSummaryBean>>() {
            @Override
            public void onSuccess(SearchResultsBean<OrganizationSummaryBean> response) {
                orgBeans = response.getBeans();
                dataPacketLoaded();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#renderPage()
     */
    @Override
    protected void renderPage() {
        if (query != null) {
            searchBox.setValue(query);
        } else {
            searchBox.setValue(""); //$NON-NLS-1$
        }
        orgs.setMemberOrgs(getCurrentUserOrgs());
        orgs.setValue(orgBeans);

        String dashHref = navHelper.createHrefToPage(DashboardPage.class, MultimapUtil.emptyMap());
        breadcrumb.addItem(dashHref, "home", i18n.format(AppMessages.HOME)); //$NON-NLS-1$
        breadcrumb.addActiveItem("search", i18n.format(AppMessages.ORGANIZATIONS)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_CONSUME_ORGS);
    }

}
