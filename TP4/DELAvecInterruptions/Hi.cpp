// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */


// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)
 
 



 #include <avr/interrupt.h> 
 #include <avr/io.h> 

 #define F_CPU 80000000
 #include <util/delay.h>
volatile uint8_t etat;
uint8_t  compteur = 0;
	
	uint8_t relache = 0b100; //bouton poussoir
	
	uint8_t appuye = 0; // bouton poussoir
	
	uint8_t rouge = 0b10; //Led prend la couleur rouge
	
	uint8_t vert = 0b01; //Led prend la couleur verte
	
	uint8_t sansCouleur = 0b00; // Led eteinte
	
	uint8_t initCompteur = 0; // initialisation du compteur
void myDelay(uint8_t n){
	while(n--)
	{
		_delay_us(1);
	}
	}
void initialisation(){
	cli();
	//PORTA = 0;
	
	
	uint8_t	tempsH;
	uint8_t	tempsL = 0;
	uint8_t	tempsTot = 1000; 

 	uint8_t go = 0b10; //Led prend la couleur rouge
	
	uint8_t stop = 0; //Led prend la couleur verte
	volatile uint8_t roule; //etat present de la voiture qui, peut peut-etre dans une eventualite possible rouler
 	uint8_t sansCouleur = 0b00; // Led eteinte
	
	
   DDRA = 0xff; // PORT A est en mode sortie
   DDRB = 0xff; // PORT B est en mode sortie
   DDRC = 0xff; // PORT C est en mode sortie
   DDRD = 0x00; // PORT D est en mode entree
   
   DDRD = 1<<INT0;
   PORTD = 1<<INT0;
	
   PORTA = 0 << INT0;
	
   EIMSK |= (1 << INT0) ;
   EICRA |= (1 << ISC00); //le 00 suit le mouvement du front montant et descendant
   // le 01 ne fait que suivre le front descendant ---> ne respecte pas le socnditions du tp ne pas prendre poour le probleme 1
   //le 10 fait l'entierete des etapes en continu sans besoin d'appuyere ----> ne prend pas les conditions if()
   //11 same thing
//multiple cases ?


	sei();
	}

ISR(INT0_vect){
		
		_delay_ms( 30 );
			EIFR |= (1 << INTF0);//ou 0	 
		
		
	switch(etat)
    {
		case 0 : 
			etat = 0;
			PORTA = rouge;
		
			
				etat++;
			
		break;
		
		case 1 :
		
			
				_delay_ms(10);
				PORTA = vert;
				etat++;
			
		break;
		
		case 2 :
		
			
				_delay_ms(10);
				PORTA = rouge;
				etat++;
			
		break;
		
		case 3 :
		
			
				_delay_ms(10);
				PORTA = sansCouleur;
				etat++;
			
		break;
		
		case 4 :
		
			
				_delay_ms(10);
				PORTA = vert;
				etat++;
			
		break;
		
		case 5 :
		
			
				_delay_ms(10);
				PORTA = rouge;
				etat = 0;
				 
		break;
		EIFR |= (0 << INTF0);//ou 0	 
	}
  }
		
	
		
	 


 
 int main()
 {
	initialisation();
	etat = 0;
	for(;;)
	{
		
	}
	
 	
	
	
	
   return 0; 
 }

