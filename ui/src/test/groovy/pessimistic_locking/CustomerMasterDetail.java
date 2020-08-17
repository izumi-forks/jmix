package pessimistic_locking;

import io.jmix.ui.action.list.EditAction;
import io.jmix.ui.component.Button;
import io.jmix.ui.component.HBoxLayout;
import io.jmix.ui.component.Table;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;
import test_support.entity.sales.Customer;

import javax.inject.Named;

@UiController("test$Customer.md")
@UiDescriptor("customer-md.xml")
@LookupComponent("table")
@LoadDataBeforeShow
public class CustomerMasterDetail extends MasterDetailScreen<Customer> {

    @Autowired
    CollectionContainer<Customer> customersDc;
    @Autowired
    Table<Customer> table;
    @Named("table.edit")
    EditAction<Customer> tableEdit;
    @Autowired
    Button cancelBtn;
    @Autowired
    HBoxLayout actionsPane;
}