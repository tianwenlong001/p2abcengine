﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using ABC4TrustSmartCard;
using UProveCrypto;

namespace abc4trust_uprove
{
  public sealed class DeviceManager : IDisposable
  {

    public DeviceManager(SmartCardParams smartCardParams, ParameterSet parameterSet, bool useVirtualDevice = true)
    {
      this.UseVirtualDevice = useVirtualDevice;
      this.parameterSet = parameterSet;
      if (smartCardParams == null)
      {
        this.smartCardParam = new SmartCardParams("1234", 1, 0, 1);
      }
      else
      {
        this.smartCardParam = smartCardParams;
      }
    }
    /*
    public DeviceManager(bool useVirtualDevice = true)
    {
      new DeviceManager(null, useVirtualDevice);
    }
     * */

    public GroupDescription Gq { get; set; }
    public string HashFunctionOID { get; set; }
    public HashFunction hash { get; set; }
    private SmartCardParams smartCardParam;
    public ParameterSet parameterSet { get; set; }
    public GroupElement Gd;
    private IDevice device = null;
    //private static byte[] deviceSecret = new byte[] { 1, 2, 3, 4, 5 }; // this secret will be generated by the device.
    private static byte[] deviceSecret = new byte[] { 42 }; // this secret will be generated by the device.
    private bool UseVirtualDevice = true; //set to false to integrate with a real smartcard
    public bool IsVirtualDevice { get { return UseVirtualDevice; } }

#if DEBUG
    public PresentationProof pDebug { get; set; }
#endif

    public void Dispose()
    {
      hash.Dispose();
      device.Dispose();
    }

    public void EnsureDeviceInit()
    {
      if (device == null)
      {
        GetDevice();
      }
    }

    public IDevice GetDevice()
    {
      if (device == null)
      {
        InitDevice(this.parameterSet);
      }
      return device;
    }

    private void InitDevice(ParameterSet set)
    {

      if (device == null)
      {
        if (UseVirtualDevice)
        {
          device = new VirtualDevice(set, new BigInteger(1, deviceSecret));
        }
        else
        {
          // use a smartcard
          device = new SmartCardDevice(set.Group, set.Gd, smartCardParam);
        }
        Gq = set.Group;
        HashFunctionOID = SecurityLevelUtils.getHashfunction(set);
        hash = new HashFunction(HashFunctionOID);
        Gd = set.Gd;
      }
    }

    /*
    // TODO IS THIS USED BY ANY ONE?
    private void InitDevice(IssuerParameters ip)
    {
      if (device == null)
      {
        if (UseVirtualDevice)
        {
          device = new VirtualDevice(ip, new BigInteger(1, deviceSecret));
        }
        else
        {
          // use a smartcard
          device = new SmartCardDevice(ip.Gq, ip.Gd, smartCardParam);
        }
        Gq = ip.Gq;
        HashFunctionOID = ip.HashFunctionOID;
        hash = new HashFunction(HashFunctionOID);
        Gd = ip.Gd;
      }

      // TODO: what to do if it's already initialized? silently replace? throw? (this will happen if multiple credentials are created).
    }
    */

    private BigInteger ComputeDeviceChallengeHardware(byte[] messageForDevice)
    {
      this.hash.Hash((byte)1);
      this.hash.Hash(messageForDevice);
      //this.hash.Hash(null);
      byte[] digest = this.hash.Digest;
      if (digest.Length <= 32)
      {
        return new BigInteger(1, digest);
      }
      else
      {
        byte[] digestTrunk = new byte[32];
        Buffer.BlockCopy(digest, 0, digestTrunk, 0, 32);
        return new BigInteger(1, digestTrunk);
      }
    }

    private BigInteger ComputeDeviceChallengeVirtual(byte[] messageForDevice)
    {
      BigInteger q = Gq.Q; ;
      this.hash.Hash(messageForDevice);
      this.hash.Hash(null);
      return new BigInteger(1, this.hash.Digest) % q;
    }

    internal BigInteger ComputeDeviceChallenge(byte[] messageForDevice)
    {
      if (IsVirtualDevice)
      {
        return this.ComputeDeviceChallengeVirtual(messageForDevice);
      }
      else
      {
        return this.ComputeDeviceChallengeHardware(messageForDevice);
      }
    }

    internal byte[] SetIssueCredential()
    {
      this.EnsureDeviceInit();
      SmartCardDevice d = (SmartCardDevice)device;
      SmartCard s = d.Device;
      byte[] proofSession = s.BeginCommitment(this.smartCardParam.proverID);
      s.GetIssuanceCommitment((byte)this.smartCardParam.credID);
      s.GetIssuanceResponse((byte)this.smartCardParam.credID);
      return proofSession;
    }

  }
  
}