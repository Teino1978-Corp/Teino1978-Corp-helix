package com.linkedin.clustermanager.agent.file;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.linkedin.clustermanager.ClusterDataAccessor.ClusterPropertyType;
import com.linkedin.clustermanager.ClusterManagementService;
import com.linkedin.clustermanager.ZNRecord;
import com.linkedin.clustermanager.store.PropertyStoreException;
import com.linkedin.clustermanager.store.file.FilePropertyStore;
import com.linkedin.clustermanager.tools.StateModelConfigGenerator;
import com.linkedin.clustermanager.util.CMUtil;

public class FileClusterManagementTool implements ClusterManagementService
{
  private static Logger LOG = Logger.getLogger(FileClusterManagementTool.class);
  private final FilePropertyStore<ZNRecord> _store; 
  
  public FileClusterManagementTool(FilePropertyStore<ZNRecord> store)
  {
    _store = store;
  }
  
  @Override
  public List<String> getClusters()
  {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException(
      "getClusters() is NOT supported by FileClusterManagementTool");

  }

  // @Override
  public List<String> getNodeNamesInCluster(String clusterName)
  {
    String memberInstancesPath = CMUtil.getMemberInstancesPath(clusterName);

    List<String> childs = null;
    List<String> instanceNames = new ArrayList<String>();
    try
    {
      childs = _store.getPropertyNames(memberInstancesPath);
      for (String child : childs)
      {
        // strip memberInstancesPath from instanceName
        String instanceName = child.substring(child.lastIndexOf('/') + 1);
        instanceNames.add(instanceName);
        
      }
      return instanceNames;  
    }
    catch (PropertyStoreException e)
    {
      LOG.error("Fail to getNodeNamesInCluster, cluster " + clusterName + 
          "\nexception: " + e);
    }
    
    return null;
  }

  @Override
  public List<String> getResourceGroupsInCluster(String clusterName)
  {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException(
      "getResourceGroupsInCluster() is NOT supported by FileClusterManagementTool");

  }

  @Override
  public void addCluster(String clusterName, boolean overwritePrevRecord) 
  {
    try
    {
      /**
      if (_store.getProperty(path) != null)
      {
        LOG.warn("Target directory exists.Cleaning the target directory:" + path
            + " overwritePrevRecord: " + overwritePrevRecord);
        if (overwritePrevRecord)
        {
          _store.removeProperty(path);
        } else
        {
          throw new PropertyStoreException("Target directory already exists, " +
              "overwritePrevRecord: " + overwritePrevRecord);
        }
      }
      **/

      _store.removeNamespace(clusterName);
      _store.createPropertyNamespace(clusterName);
      
      // IDEAL STATE
      _store.createPropertyNamespace(CMUtil.getIdealStatePath(clusterName));
      // CONFIGURATIONS
      _store.createPropertyNamespace(CMUtil.getConfigPath(clusterName));
      // LIVE INSTANCES
      _store.createPropertyNamespace(CMUtil.getLiveInstancesPath(clusterName));
      // MEMBER INSTANCES
      _store.createPropertyNamespace(CMUtil.getMemberInstancesPath(clusterName));
      // External view
      _store.createPropertyNamespace(CMUtil.getExternalViewPath(clusterName));
      // State model definition
      _store.createPropertyNamespace(CMUtil.getStateModelDefinitionPath(clusterName));
            
      StateModelConfigGenerator generator = new StateModelConfigGenerator();
      addStateModelDef(clusterName, "MasterSlave", generator.generateConfigForMasterSlave());
      
    }
    catch(PropertyStoreException e)
    {
      LOG.error("Fail to add cluster " + clusterName + "\nexception: " + e);
    }

  }

  @Override
  public void addResourceGroup(String clusterName,
                               String resourceGroup,
                               int numResources,
                               String stateModelRef)
  {
    String idealStatePath = CMUtil.getIdealStatePath(clusterName);
    String resourceGroupIdealStatePath = idealStatePath + "/" + resourceGroup;
    
    /**
    if (_zkClient.exists(dbIdealStatePath))
    {
      logger.warn("Skip the operation. DB ideal state directory exists:"
          + dbIdealStatePath);
      return;
    }
    **/
 
    ZNRecord idealState = new ZNRecord();
    idealState.setId(resourceGroup);
    idealState.setSimpleField("partitions", String.valueOf(numResources));
    idealState.setSimpleField("state_model_def_ref", stateModelRef);
    
    try
    {
      _store.setProperty(resourceGroupIdealStatePath, idealState);
    }
    catch (PropertyStoreException e)
    {
      LOG.error("Fail to add resource group, cluster:" + clusterName + 
          " resource group:" + resourceGroup +
          "\nexception: " + e);
    }
    
  }

  // @Override
  public void addNode(String clusterName, ZNRecord nodeConfig)
  {    
    String configsPath = CMUtil.getConfigPath(clusterName);
    String nodeId = nodeConfig.getId();
    String nodeConfigPath = configsPath + "/" + nodeId;

    /**
    if (_zkClient.exists(instanceConfigPath))
    {
      throw new ClusterManagerException("Node " + nodeId
          + " already exists in cluster " + clusterName);
    }
    **/

    try
    {
      // ZKUtil.createChildren(_zkClient, instanceConfigsPath, nodeConfig);
      _store.setProperty(nodeConfigPath, nodeConfig);
  
      // _zkClient.createPersistent(CMUtil.getMessagePath(clusterName, nodeId), true);
      _store.createPropertyNamespace(CMUtil.getMessagePath(clusterName, nodeId));
      
      // _zkClient.createPersistent(CMUtil.getCurrentStateBasePath(clusterName, nodeId), true);
      _store.createPropertyNamespace(CMUtil.getCurrentStateBasePath(clusterName, nodeId));
      
      // _zkClient.createPersistent(CMUtil.getErrorsPath(clusterName, nodeId), true);
      _store.createPropertyNamespace(CMUtil.getErrorsPath(clusterName, nodeId));
      
      // _zkClient.createPersistent(CMUtil.getStatusUpdatesPath(clusterName, nodeId), true);
      _store.createPropertyNamespace(CMUtil.getStatusUpdatesPath(clusterName, nodeId));
    }
    catch(Exception e)
    {
      LOG.error("Fail to add node, cluster:" + clusterName + 
          "\nexception: " + e);
    }
    
  }

  @Override
  public ZNRecord getResourceGroupIdealState(String clusterName, String resourceGroupName)
  {    
    String resourceGroupPath = CMUtil.getClusterPropertyPath(clusterName, ClusterPropertyType.IDEALSTATES) + "/" + resourceGroupName;
    try
    {
      ZNRecord idealState = _store.getProperty(resourceGroupPath);
      return idealState;
    }
    catch (Exception e)
    {
      LOG.error("Fail to getResourceGroupIdealState, cluster:" + clusterName + 
          " resourceGroup:" + resourceGroupName + 
          "\nexception: " + e);
    }
    return null;
  }

  @Override
  public void setResourceGroupIdealState(String clusterName, String resourceGroupName,
                                         ZNRecord newIdealState)
  {
    String resourceGroupPath = CMUtil.getClusterPropertyPath(clusterName, ClusterPropertyType.IDEALSTATES) + "/" + resourceGroupName;
    try
    {
      _store.setProperty(resourceGroupPath, newIdealState);
    }
    catch (Exception e)
    {
      LOG.error("Fail to setResourceGroupIdealState, cluster:" + clusterName + 
          " resourceGroup:" + resourceGroupName + 
          "\nexception: " + e);
    }

  }

  @Override
  public void enableInstance(String clusterName, String instanceName, boolean enabled)
  {
    throw new UnsupportedOperationException(
      "enableInstance() is NOT supported by FileClusterManagementTool");
  }

  @Override
  public void addStateModelDef(String clusterName, String stateModelDef, ZNRecord record)
  {
    
    String stateModelDefPath = CMUtil.getStateModelDefinitionPath(clusterName);
    String stateModelPath = stateModelDefPath + "/" + stateModelDef;
    /**
    if (_zkClient.exists(stateModelPath))
    {
      logger.warn("Skip the operation.State Model directory exists:"
          + stateModelPath);
      throw new ClusterManagerException("State model path " + stateModelPath
          + " already exists.");
    }
    **/
    
    try
    {
      _store.setProperty(stateModelPath, record);
    }
    catch (PropertyStoreException e)
    {
      LOG.error("Fail to addStateModelDef, cluster:" + clusterName + 
          " stateModelDef:" + stateModelDef + 
          "\nexception: " + e);
    }
    
  }

  @Override
  public void dropResourceGroup(String clusterName, String resourceGroup)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
      "dropResourceGroup() is NOT supported by FileClusterManagementTool");
  }

  @Override
  public List<String> getStateModelDefs(String clusterName)
  {
    // TODO Auto-generated method stub
    // return null;
    throw new UnsupportedOperationException(
      "getStateModelDefs() is NOT supported by FileClusterManagementTool");
  }

  @Override
  public List<String> getInstancesInCluster(String clusterName)
  {
    throw new UnsupportedOperationException(
        "getInstancesInCluster() is NOT supported by FileClusterManagementTool");
  }

  @Override
  public ZNRecord getInstanceConfig(String clusterName, String instanceName)
  {
    throw new UnsupportedOperationException(
        "getInstanceConfig() is NOT supported by FileClusterManagementTool");
  }

  @Override
  public void addInstance(String clusterName, ZNRecord instanceConfig)
  {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
        "addInstance() is NOT supported by FileClusterManagementTool");
  }

  @Override
  public ZNRecord getStateModelDef(String clusterName, String stateModelName)
  {
    throw new UnsupportedOperationException(
      "getStateModelDef() is NOT supported by FileClusterManagementTool");

    // return null;
  }

  @Override
  public ZNRecord getResourceGroupExternalView(String clusterName, String resourceGroup)
  {
    throw new UnsupportedOperationException(
        "getResourceGroupExternalView() is NOT supported by FileClusterManagementTool");
  }

}