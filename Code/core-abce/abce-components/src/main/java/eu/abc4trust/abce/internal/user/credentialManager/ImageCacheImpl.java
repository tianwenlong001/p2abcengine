//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*/**/****************************************************************

package eu.abc4trust.abce.internal.user.credentialManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import com.google.inject.Inject;

import eu.abc4trust.util.StorageUtil;

public class ImageCacheImpl implements ImageCache {

    private final static Logger logger = Logger.getLogger(ImageCacheImpl.class.getName());

    private final URL DEFAULT_IMAGE;
    private final ImageCacheStorage imStore;

    @Inject
    public ImageCacheImpl(ImageCacheStorage imStore) {
        this.imStore = imStore;
        URL url;
        try {
            url = new URL("file://" + imStore.getDefaultImagePath());
        } catch (MalformedURLException ex) {
            url = null;
        }
        this.DEFAULT_IMAGE = url;
    }

    @Override
    public URL storeImage(URI image) throws ImageCacheException {
        try  {
            URL url = image.toURL();
            URLConnection connection = url
                    .openConnection();
            
            connection.setConnectTimeout(10000);
            connection.connect();

            InputStream reader = connection.getInputStream();

            String filetype = this.getFiletype(image);
            URL imageUrl = this.imStore.store(filetype, reader);

            StorageUtil.closeIgnoringException(reader);
            return imageUrl;
        } catch (IOException ex) {
            logger.warning("storeImage - failed : " + image + " - exception : " + ex);
            return this.DEFAULT_IMAGE;
        } catch (Exception ex) {
            throw new ImageCacheException(ex);
        }
    }

    private String getFiletype(URI image) {
        String s = null;
        if (image.getPath() != null) {
        	s = image.getPath();
        } else {
    	    s = image.getSchemeSpecificPart();
        }
        int lastIndexOfDot = s.lastIndexOf(".");
        String type = s.substring(lastIndexOfDot + 1, s.length());
        return type;
    }

    @Override
    public URL getDefaultImage() {
        return this.DEFAULT_IMAGE;
    }
}
