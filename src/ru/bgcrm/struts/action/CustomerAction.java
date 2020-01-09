package ru.bgcrm.struts.action;

import java.sql.Connection;
import java.util.List;
import java.util.SortedMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import ru.bgcrm.dao.CommonDAO;
import ru.bgcrm.dao.CustomerDAO;
import ru.bgcrm.dao.CustomerLinkDAO;
import ru.bgcrm.dao.ParamDAO;
import ru.bgcrm.dao.ParamGroupDAO;
import ru.bgcrm.dao.ParamValueDAO;
import ru.bgcrm.dao.PatternDAO;
import ru.bgcrm.dao.process.ProcessLinkDAO;
import ru.bgcrm.event.EventProcessor;
import ru.bgcrm.event.customer.CustomerRemovedEvent;
import ru.bgcrm.event.customer.CustomerChangedEvent;
import ru.bgcrm.event.link.LinkAddingEvent;
import ru.bgcrm.model.BGIllegalArgumentException;
import ru.bgcrm.model.BGMessageException;
import ru.bgcrm.model.CommonObjectLink;
import ru.bgcrm.model.Customer;
import ru.bgcrm.model.SearchResult;
import ru.bgcrm.model.param.Parameter;
import ru.bgcrm.model.param.ParameterAddressValue;
import ru.bgcrm.model.param.ParameterEmailValue;
import ru.bgcrm.model.param.ParameterPhoneValue;
import ru.bgcrm.model.param.ParameterPhoneValueItem;
import ru.bgcrm.model.param.ParameterValuePair;
import ru.bgcrm.plugin.bgbilling.dao.CommonContractDAO;
import ru.bgcrm.plugin.bgbilling.model.CommonContract;
import ru.bgcrm.struts.form.DynActionForm;
import ru.bgcrm.util.Utils;
import ru.bgcrm.util.sql.ConnectionSet;
import ru.bgcrm.util.sql.SingleConnectionConnectionSet;

public class CustomerAction
	extends BaseAction
{
	@Override
	protected ActionForward unspecified( ActionMapping mapping,
										 DynActionForm form,
										 HttpServletRequest request,
										 HttpServletResponse response,
										 ConnectionSet conSet )
		throws Exception
	{
		return customer( mapping, form, request, response, conSet );
	}

	public ActionForward customerCreate( ActionMapping mapping,
										 DynActionForm form,
										 HttpServletRequest request,
										 HttpServletResponse response,
										 ConnectionSet conSet )
		throws Exception
	{
		String title = form.getParam( "title", "Новый контрагент" );

		Customer customer = new Customer();
		customer.setTitle( title );

		new CustomerDAO( conSet.getConnection() ).updateCustomer( customer );

		form.getResponse().setData( "customer", customer );

		return processJsonForward( conSet, form, response );
	}

	public ActionForward customerGet( ActionMapping mapping,
									  DynActionForm form,
									  HttpServletRequest request,
									  HttpServletResponse response,
									  ConnectionSet conSet )
		throws Exception
	{
		Connection con = conSet.getConnection();
		
		CustomerDAO customerDAO = new CustomerDAO( con );
		PatternDAO patternDAO = new PatternDAO( con );
		ParamGroupDAO groupDAO = new ParamGroupDAO( con );

		Customer customer = customerDAO.getCustomerById( form.getId() );
		if( customer != null )
		{
			customer.setGroupIds( customerDAO.getGroupIds( form.getId() ) );
			form.getResponse().setData( "customer", customer );

			// TODO: Переделать на кэш.
			request.setAttribute( "patternList", patternDAO.getPatternList( Customer.OBJECT_TYPE ) );
			request.setAttribute( "parameterGroupList", groupDAO.getParameterGroupList( Customer.OBJECT_TYPE ) );
		}

		return processUserTypedForward( conSet, mapping, form, response, "edit" );
	}

	public ActionForward customerUpdate( ActionMapping mapping,
										 DynActionForm form,
										 HttpServletRequest request,
										 HttpServletResponse response,
										 ConnectionSet conSet )
		throws Exception
	{
		CustomerDAO customerDAO = new CustomerDAO( conSet.getConnection(), true, form.getUserId() );

		Customer customer = customerDAO.getCustomerById( form.getId() );
		if( customer == null )
		{
			throw new BGMessageException( "Контрагент не найден." );
		}

		String titleBefore = Utils.maskNull( customer.getTitle() );

		customer.setTitle( form.getParam( "title" ) );
		customer.setTitlePattern( form.getParam( "titlePattern", "" ) );
		customer.setTitlePatternId( form.getParamInt( "titlePatternId", -1 ) );
		customer.setParamGroupId( Utils.parseInt( form.getParam( "parameterGroupId" ) ) );
		customer.setGroupIds( form.getSelectedValues( "customerGroupId" ) );

		if( Utils.isBlankString( customer.getTitle() ) &&
			customer.getTitlePatternId() <= 0 &&
			Utils.isBlankString( customer.getTitlePattern() ) )
		{
			throw new BGIllegalArgumentException();
		}

		customerDAO.updateCustomerTitle( titleBefore, customer, -1, form.getResponse() );
		customerDAO.updateGroupIds( customer.getId(), form.getSelectedValues( "customerGroupId" ) );

		CustomerChangedEvent updateEvent = new CustomerChangedEvent( form, form.getId() );
		EventProcessor.processEvent( updateEvent, conSet );

		return processJsonForward( conSet, form, response );
	}

	public ActionForward customerDelete( ActionMapping mapping,
										 DynActionForm form,
										 HttpServletRequest request,
										 HttpServletResponse response,
										 ConnectionSet conSet )
		throws Exception
	{
		Connection con = conSet.getConnection();
		
		new CustomerDAO( con ).deleteCustomer( form.getId() );
		new ParamValueDAO( con ).deleteParams( Customer.OBJECT_TYPE, form.getId() );
		new CustomerLinkDAO( con ).deleteObjectLinks( form.getId() );

		CustomerRemovedEvent deleteEvent = new CustomerRemovedEvent( form, form.getId() );
		EventProcessor.processEvent( deleteEvent, new SingleConnectionConnectionSet( con ) );

		return processJsonForward( conSet, form, response );
	}

	public ActionForward customer( ActionMapping mapping,
								   DynActionForm form,
								   HttpServletRequest request,
								   HttpServletResponse response,
								   ConnectionSet conSet )
		throws Exception
	{
		CustomerDAO customerDAO = new CustomerDAO( conSet.getConnection() );

		Customer customer = customerDAO.getCustomerById( form.getId() );
		if( customer != null )
		{
			customer.setGroupIds( customerDAO.getGroupIds( form.getId() ) );
			form.getResponse().setData( "customer", customer );
		}

		return processUserTypedForward( conSet, mapping, form, response, FORWARD_DEFAULT );
	}

	public ActionForward customerTitleList( ActionMapping mapping,
											DynActionForm form,
											HttpServletRequest request,
											HttpServletResponse response,
											ConnectionSet conSet )
		throws Exception
	{
		List<String> titles = new CustomerDAO( conSet.getConnection() ).getCustomerTitles( CommonDAO.getLikePattern( form.getParam( "title" ), form.getParam( "mode", "subs" ) ),
		                                                                                   setup.getInt( "customer.search.by.title.count", 10 ) );
		form.getResponse().setData( "list", titles );

		return processUserTypedForward( conSet, mapping, form, response, FORWARD_DEFAULT );
	}

	public ActionForward customerMerge( ActionMapping mapping,
										DynActionForm form,
										HttpServletRequest request,
										HttpServletResponse response,
										ConnectionSet conSet )
		throws Exception
	{
		Integer customerId = form.getParamInt( "customerId" );
		Integer mergingCustomerId = form.getParamInt( "mergingCustomerId" );
		
		Connection con = conSet.getConnection();

		ParamValueDAO paramValueDAO = new ParamValueDAO( con );
		ParamDAO paramDAO = new ParamDAO( con, form.getUserId() );
		CustomerLinkDAO customerLinkDAO = new CustomerLinkDAO( con );
		ProcessLinkDAO processLinkDAO = new ProcessLinkDAO( con );
		CommonContractDAO commonContractDAO = new CommonContractDAO( con );
		CustomerDAO customerDAO = new CustomerDAO( con );
		
		SearchResult<Parameter> searchResult = new SearchResult<Parameter>();
		paramDAO.getParameterList( searchResult, Customer.OBJECT_TYPE, "", 0, null );

		List<Parameter> customerParameterList = searchResult.getList();

		List<ParameterValuePair> customerParamValues = paramValueDAO.loadParameters( customerParameterList, customerId, true );
		List<ParameterValuePair> mergingCustomerParamValues = paramValueDAO.loadParameters( customerParameterList, mergingCustomerId, true );

		//копирование параметров контрагента
		for( Parameter param : customerParameterList )
		{
			String type = param.getType();
			Object paramCustomerValue = "";
			Object paramMergingCustomerValue = "";

			int paramId = param.getId();

			for( ParameterValuePair customerPVP : customerParamValues )
			{
				if( customerPVP.getParameter().getId() == paramId )
				{
					paramCustomerValue = customerPVP.getValue();
					break;
				}
			}

			for( ParameterValuePair mergingCustomerPVP : mergingCustomerParamValues )
			{
				if( mergingCustomerPVP.getParameter().getId() == paramId )
				{
					paramMergingCustomerValue = mergingCustomerPVP.getValue();
					break;
				}
			}

			if( paramCustomerValue != null && paramMergingCustomerValue != null )
			{
				//логика мерджа

				boolean isMultiple = param.getConfigMap().getBoolean( Parameter.PARAM_MULTIPLE_KEY, false );

				if( Parameter.TYPE_ADDRESS.equals( type ) && isMultiple )
				{

					SortedMap<Integer, ParameterAddressValue> customerAddressMap = paramValueDAO.getParamAddress( customerId, paramId );
					SortedMap<Integer, ParameterAddressValue> mergingCustomerAddressMap = paramValueDAO.getParamAddress( mergingCustomerId, paramId );

					for( ParameterAddressValue addressValue : mergingCustomerAddressMap.values() )
					{
						boolean exist = false;
						for( ParameterAddressValue existAddressValue : customerAddressMap.values() )
						{
							if( existAddressValue.equals( addressValue ) )
							{
								exist = true;
							}
						}

						if( !exist )
						{
							paramValueDAO.updateParamAddress( customerId, paramId, 0, addressValue );
						}
					}
				}
				else if( Parameter.TYPE_PHONE.equals( type ) )
				{
					ParameterPhoneValue customerPhoneValue = paramValueDAO.getParamPhone( customerId, paramId );
					ParameterPhoneValue mergingCustomerPhoneValue = paramValueDAO.getParamPhone( mergingCustomerId, paramId );

					for( ParameterPhoneValueItem phoneItem : mergingCustomerPhoneValue.getItemList() )
					{
						boolean exist = false;
						for( ParameterPhoneValueItem existPhoneValue : customerPhoneValue.getItemList() )
						{
							if( existPhoneValue.equals( phoneItem ) )
							{
								exist = true;
							}
						}

						if( !exist )
						{
							customerPhoneValue.addItem( phoneItem );
						}
					}

					paramValueDAO.updateParamPhone( customerId, paramId, customerPhoneValue );

				}
				else if( Parameter.TYPE_EMAIL.equals( type ) && isMultiple )
				{
					SortedMap<Integer, ParameterEmailValue> mergingCustomerEmailMap = paramValueDAO.getParamEmail( mergingCustomerId, paramId );
					SortedMap<Integer, ParameterEmailValue> customerEmailMap = paramValueDAO.getParamEmail( customerId, paramId );

					for( ParameterEmailValue emailValue : mergingCustomerEmailMap.values() )
					{
						boolean exist = false;
						for( ParameterEmailValue existEmailValue : customerEmailMap.values() )
						{
							if( existEmailValue.equals( emailValue ) )
							{
								exist = true;
							}
						}

						if( !exist )
						{
							paramValueDAO.updateParamEmail( customerId, paramId, 0, emailValue );
						}
					}
				}
				else if( !paramCustomerValue.equals( paramMergingCustomerValue ) )
				{
					throw new BGMessageException( "Параметр '" + param.getTitle() + "' должны совпадать" );
				}
			}
			else if( paramMergingCustomerValue != null )
			{
				paramValueDAO.copyParam( mergingCustomerId, customerId, paramId );
			}
			//оба null, оба пустые или менять не надо
		}

		//копирование привязанных сущностей
		for( CommonObjectLink link : customerLinkDAO.getObjectLinksWithType( mergingCustomerId, "" ) )
		{
			customerLinkDAO.deleteLink( link );

			link.setObjectId( customerId );
			link.setObjectType( "customer" );

			LinkAddingEvent event = new LinkAddingEvent( form, link );
			EventProcessor.processEvent( event, new SingleConnectionConnectionSet( con ) );

			customerLinkDAO.addLink( link );
		}

		//customerLinkDAO.copyLinks( mergingCustomerId, customerId, "" );
		//customerLinkDAO.deleteObjectLinks( mergingCustomerId );

		processLinkDAO.linkToAnotherObject( mergingCustomerId, "customer", customerId, "customer", "", "" );

		for( CommonContract commonContract : commonContractDAO.getContractList( mergingCustomerId ) )
		{
			commonContractDAO.changeCustomerLink( commonContract.getId(), customerId );
		}

		EventProcessor.processEvent( new CustomerChangedEvent( form, customerId ), conSet );
		
		/*sphinxDAO.delete( mergingCustomerId );
		sphinxDAO.customerCacheUpdate( con, customerId );*/

		EventProcessor.processEvent(  new CustomerRemovedEvent( form, mergingCustomerId ), conSet );
		
		//удаление контрагента
		customerDAO.deleteCustomer( mergingCustomerId );
		new CustomerLinkDAO( con ).deleteObjectLinks( mergingCustomerId );
		
		return processJsonForward( con, form, response );
	}

	/*public ActionForward customerProcessList( ActionMapping mapping,
	                                          DynActionForm form,
	                                          HttpServletRequest request,
	                                          HttpServletResponse response,
	                                          ConnectionSet conSet )
	    throws SQLException, BGException
	{
		Set<Integer> allowedProcesTypeIds = Utils.toIntegerSet( form.getPermission().get( "allowedProcessTypeIds" ) );

		ProcessDAO processDAO = new ProcessDAO( con );
		Customer customer = new CustomerDAO( con ).getCustomerById( form.getParamInt( "customerId", -1 ) );

		SearchResult<Process> searchResult = new SearchResult<Process>();
		processDAO.searchProcessForCustomer( searchResult, customer.getId(), null );

		ProcessTypeDAO processTypeDAO = new ProcessTypeDAO( con );
		Map<Integer, ProcessType> processTypeMap = processTypeDAO.getFullProcessTypeMap();
		Iterator<Entry<Integer, ProcessType>> it = processTypeMap.entrySet().iterator();
		List<Integer> idsToRemove = new ArrayList<Integer>();

		if( allowedProcesTypeIds.size() != 0 )
		{
			// TODO: сделано криво в два цикла из-за ConcurrentModificationException. придумать как сделать нормально
			while( it.hasNext() )
			{
				Entry<Integer, ProcessType> keyValue = it.next();
				if( !allowedProcesTypeIds.contains( keyValue.getValue().getId() ) ) idsToRemove.add( keyValue.getValue().getId() );
			}

			for( Integer idToRemove : idsToRemove )
			{
				processTypeMap.remove( idToRemove );
			}
		}

		form.getResponse().setData( "customer", customer );
		form.getResponse().setData( "process", searchResult );
		form.getResponse().setData( "processTypeMap", processTypeMap );

		return processUserTypedForward( mapping, form, response, "customerProcessList" );
	}

	public ActionForward customerCreateProcess( ActionMapping mapping,
	                                            DynActionForm form,
	                                            HttpServletRequest request,
	                                            HttpServletResponse response,
	                                            ConnectionSet conSet )
	    throws BGException
	{
		try
		{
			ParameterMap permission = form.getPermission();
			Set<Integer> processTypeIds = Utils.toIntegerSet( permission.get( "allowedProcessTypeIds" ) );
			Integer typeId = form.getParamInt( "typeId", -1 );

			if( processTypeIds.contains( typeId ) || processTypeIds.size() == 0 )
			{

				CustomerDAO customerDAO = new CustomerDAO( con );
				Customer customer = customerDAO.getCustomerById( form.getParamInt( "customerId", -1 ) );

				form.setParam( "typeId", String.valueOf( typeId ) );

				Process process = ProcessAction.processCreate( form, con, request );

				form.setParam( "id", String.valueOf( process.getId() ) );
				form.setParam( "objectType", "process" );
				form.setParam( "linkedObjectType", "customer" );
				form.setParam( "linkedObjectId", String.valueOf( customer.getId() ) );
				form.setParam( "linkedObjectTitle", customer.getTitle() );

				LinkAction.addLink( form, request, con );

				form.getResponse().setData( "process", process );
			}
		}
		catch( BGMessageException e )
		{
			setError( form, e.getMessage() );
		}
		catch( SQLException e )
		{
			setError( form, e.getMessage() );
		}

		return processJsonForward( form, response );
	}*/

	protected void formatCustomerTitle( Customer customer,
										CustomerDAO customerDAO,
										ParamValueDAO paramDAO,
										Connection con )
		throws Exception
	{
		PatternDAO patternDAO = new PatternDAO( con );
		String titlePattern = customer.getTitlePattern();
		if( customer.getTitlePatternId() > 0 )
		{
			ru.bgcrm.model.param.Pattern pattern = patternDAO.getPattern( customer.getTitlePatternId() );
			if( pattern != null )
			{
				titlePattern = pattern.getPattern();
			}
		}
		customer.setTitle( Utils.formatPatternString( Customer.OBJECT_TYPE, customer.getId(), paramDAO, titlePattern ) );
		customerDAO.updateCustomer( customer );
	}

	protected void setCustomerTitle( String title,
									 Customer customer,
									 PatternDAO patternDAO,
									 ParamValueDAO paramDAO )
		throws Exception
	{
		if( customer.getTitlePatternId() == 0 )
		{
			customer.setTitle( Utils.formatPatternString( Customer.OBJECT_TYPE, customer.getId(), paramDAO, customer.getTitlePattern() ) );
		}
		else if( customer.getTitlePatternId() > 0 )
		{
			ru.bgcrm.model.param.Pattern pattern = patternDAO.getPattern( customer.getTitlePatternId() );
			customer.setTitle( Utils.formatPatternString( Customer.OBJECT_TYPE, customer.getId(), paramDAO, pattern.getPattern() ) );
		}
		else
		{
			customer.setTitle( title );
		}
	}

	/*public ActionForward showEmailForm( ActionMapping mapping,
	                                    DynActionForm form,
	                                    HttpServletRequest request,
	                                    HttpServletResponse response )
	{
		List<String[]> emailList = getRecipientEmailList();
		request.setAttribute( "emailList", emailList );
		request.setAttribute( "action", "sendEmail" );
		request.setAttribute( "actionClass", "/contract" );
		request.setAttribute( "configMap", setup.getDataMap() );
		return mapping.findForward( FORWARD_DEFAULT );
	}*/
}
