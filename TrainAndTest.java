import java.util.*;
import java.lang.Math;
import java.io.*;
import java.lang.String;

/**
 * 
 */
public class TrainAndTest
{
	/*
	*	Parses through review to store term frequencies for each token and increments 
	*   noDocumentsContainToken HashMap if token is found.
	*
	*/

	public HashMap<String,Double>  localTokenCount;
	public HashMap<String,Double>  noDocumentsContainToken;
	public HashMap<String,Integer> featureIDMap;  
	public HashMap<Integer,String> reverseFeatureIDMap;
	public double  totalNoDocuments; 	
	public int indexCount;	
	public double maxFrequency;

	public TrainAndTest()
	{
		this.localTokenCount = new HashMap<String,Double>();
		this.noDocumentsContainToken = new HashMap<String,Double>();
		this.featureIDMap = new HashMap<String,Integer>();
		this.reverseFeatureIDMap = new HashMap<Integer,String>();
		this.totalNoDocuments = 0;
		this.indexCount=1;
	}

	public void tfIDf( ArrayList<String> tokens )
	{
		double tokenFrequency, documentsContainingToken;
		double inverseDocumentFrequency;

		Collections.sort( tokens );
		String prevToken="", token;
				
				
		for( int k=0; k<tokens.size() ;k++ )
		{
			token = tokens.get( k );
			assert token!=null;	
			assert noDocumentsContainToken != null;

			documentsContainingToken = this.noDocumentsContainToken.get( token );
			tokenFrequency = this.localTokenCount.get( token );
			inverseDocumentFrequency = Math.log( ( this.totalNoDocuments /( 1 + documentsContainingToken ) ) );
			
			if( !prevToken.equals( token ) )
			{
			  	this.localTokenCount.put( token, tokenFrequency * inverseDocumentFrequency );	
			}	
			prevToken = token;	
		}	

	}  	

    public void normalizeTokens( ArrayList<String> tokens )
	{
				Collections.sort( tokens );
				String prevToken="", token;
				double tokenFrequencyCount;		
				
				
				for( int k=0; k<tokens.size() ;k++ )
				{
					token = tokens.get( k );
					tokenFrequencyCount = this.localTokenCount.get( token );
					if( !prevToken.equals( token ) )
					{
						this.localTokenCount.put( token, tokenFrequencyCount/this.maxFrequency );
					}	
					prevToken = token;	
				}	

	}

    public void logScaleTokens( ArrayList<String> tokens )
	{
				Collections.sort( tokens );
				String prevToken="", token;
				double tokenFrequencyCount;		
				
				for( int k=0; k<tokens.size() ;k++ )
				{
					token = tokens.get( k );
					tokenFrequencyCount = this.localTokenCount.get( token );
					if( !prevToken.equals( token ) )
					{
						this.localTokenCount.put( token, 1 + Math.log( tokenFrequencyCount ) );
					}	
					prevToken = token;	
				}	

	}

	public void calculateTokenCount( String token )
	{	
		double tokenFrequencyCount;

		if( this.localTokenCount.containsKey( token ) )
		{
			tokenFrequencyCount = this.localTokenCount.get( token );
			tokenFrequencyCount++;
		}	
		else
		{	
			tokenFrequencyCount = 1;
		}	
			
		this.localTokenCount.put( token, tokenFrequencyCount );	

		// Check for max freq in document	
		if( tokenFrequencyCount > this.maxFrequency )
		{
			this.maxFrequency = tokenFrequencyCount ; 
		}
	}

	public void calculateTokenPresence( String token )
	{	
		double tokenPresence;

		if( !this.localTokenCount.containsKey( token ) )
		{
			tokenPresence = 1;
			this.localTokenCount.put( token, tokenPresence );	
		}	
	}


	public String createReviewString( ArrayList<Integer> tokenIndices, boolean deceptive )
	{
		int tokenIndex, prevTokenIndex = 0;
		String reviewString,token;

		if( deceptive )
		{
			reviewString = "+1 ";	
		}
		else
		{
			reviewString = "-1 ";	
		}	

		Collections.sort( tokenIndices );
		
		for( int j=0; j<tokenIndices.size(); j++ )
		{
			tokenIndex = tokenIndices.get( j );
			token = this.reverseFeatureIDMap.get( tokenIndex );

			if( prevTokenIndex != tokenIndex )
			{	

				reviewString += tokenIndex+":"+this.localTokenCount.get( token )+" ";
			}
			prevTokenIndex = tokenIndex;
		}
		reviewString += "\n";	
		return reviewString;	
	}

	public void buildFeatureVector( String filename, boolean deceptive, String scheme, String tfScheme )
	{
		String token, reviewLine;
		int tokenIndex;
		double tokenFrequencyCount;
	
		try
		{
  			FileReader reviewDocumentStream = new FileReader( filename );
  			BufferedReader reviewDocument   = new BufferedReader( reviewDocumentStream );

  			FileWriter featuresStream = new FileWriter( "features.txt", true );
  			BufferedWriter featuresOut = new BufferedWriter( featuresStream );

  			FileWriter inputStream = new FileWriter( "input.txt", true );
  			BufferedWriter inputOut = new BufferedWriter( inputStream );

  			while ( ( reviewLine = reviewDocument.readLine() ) != null ) 
  			{
				
				Tokenizer tk = new Tokenizer( scheme, reviewLine );
				ArrayList<String> tokens = tk.Tokenize() ; 
				ArrayList<Integer> tokenIndices = new ArrayList<Integer>();
				
				this.maxFrequency=0;

				for( int i=0; i<tokens.size() ;i++ )
				{
					token = tokens.get( i );
					if( !this.featureIDMap.containsKey( token ) )
					{
						tokenIndex = this.indexCount;
						this.featureIDMap.put( token, tokenIndex );
						this.reverseFeatureIDMap.put( tokenIndex, token );	
						this.indexCount++;

						//Write the feature out to the dictionary
						featuresOut.write( token + "\n" );
					} 
					else
					{
						tokenIndex = this.featureIDMap.get( token );
					}	
					tokenIndices.add( tokenIndex );

					if( tfScheme.equals( "RAW_FREQUENCY" ) 
						|| tfScheme.equals( "NORMALIZED_FREQUENCY" ) 
						|| tfScheme.equals(  "LOGSCALED_FREQUENCY")	 )
					{
						calculateTokenCount( token );
					}
					else
					{
						// TOKEN PRESENCE 
						calculateTokenPresence( token );
					}
				}

				if( tfScheme.equals("NORMALIZED_FREQUENCY") )
				{	
					normalizeTokens( tokens );
				}

				if( tfScheme.equals("LOGSCALED_FREQUENCY") )
				{
					logScaleTokens( tokens );
				}	

				tfIDf( tokens );
				
				String reviewString = createReviewString( tokenIndices, deceptive );

				inputOut.write( reviewString );
				this.localTokenCount.clear();

			}
			featuresOut.write( "TOTAL NO OF DOCUMENTS : "+this.totalNoDocuments+"" );
			inputOut.close();	
  			featuresOut.close();
  		}
  		catch ( Exception e ) 
  		{
  			e.printStackTrace();
  		}		
	}

	public void deletePreviousFiles()
	{
		try
		{
			File inputFile = new File( "input.txt" );
			File featuresFile = new File( "features.txt" );

			if( inputFile.exists() )
			{
				inputFile.delete();
			}	
			if( featuresFile.exists() )
			{
				featuresFile.delete();
			}	
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}

	public void DocumentData( ArrayList<String> tokens )
	{
		String token, prevToken="";
		double documentsContainingToken;

		Collections.sort( tokens );

		for( int i=0; i<tokens.size() ;i++ )
		{
			token = tokens.get( i );
			// Also increment hashmap noDocumentsContainToken for every token encountered 
			if( this.noDocumentsContainToken.containsKey( token ) )
			{
				documentsContainingToken = this.noDocumentsContainToken.get( token );
			}	
			else
			{	
				documentsContainingToken = 0;
			}	

			if( !prevToken.equals( token ) )
			{	
		  		this.noDocumentsContainToken.put( token, documentsContainingToken + 1 );
		  	}	
		  	prevToken = token;	
		}
	}

	public void documentLevelData( String filename, String scheme )
	{
		String reviewLine;
		try
		{
			FileReader reviewDocumentStream = new FileReader( filename );
			BufferedReader reviewDocument   = new BufferedReader( reviewDocumentStream );

			while ( ( reviewLine = reviewDocument.readLine() ) != null ) 
			{
				Tokenizer tk = new Tokenizer( scheme, reviewLine );
				ArrayList<String> tokens = tk.Tokenize() ; 
				DocumentData( tokens );
				this.totalNoDocuments++;
			}	
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}	
	}

	public static void main( String[] args )
	{
		if( args.length < 2 )
		{
			System.out.println("Insufficient Arguments\n SYNTAX : java TrainAndTest <tokenization_scheme> <tfScheme>");
			System.exit(-1);
		}
		String scheme = args[0];
		String tfScheme = args[1]; 	//	 RAW_FREQUENCY, NORMALIZED_FREQUENCY, LOGSCALED_FREQUENCY, TOKEN_PRESENCE
		TrainAndTest tt = new TrainAndTest();
		String truthful = "hotel_truthful_new", deceptive = "hotel_deceptive_new";

		tt.documentLevelData( truthful, scheme );
		tt.documentLevelData( deceptive, scheme );
		
		tt.deletePreviousFiles();
		tt.buildFeatureVector( truthful, false, scheme, tfScheme );
		tt.buildFeatureVector( deceptive, true, scheme, tfScheme );	
	}
}
