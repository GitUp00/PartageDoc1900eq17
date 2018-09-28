// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */


// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)
 
 




 #include <avr/io.h> 

 #define F_CPU 80000000
 #include <util/delay.h>

void myDelay(uint8_t n){
	while(n--)
	{
		_delay_us(1);
	}
	}
 int main()
 {

 	uint8_t	tempsH;
	uint8_t	tempsL = 0;
	uint8_t	tempsTot = 1000; 

 	uint8_t rouge = 0b10; //Led prend la couleur rouge
	
	uint8_t vert = 0b01; //Led prend la couleur verte
	
 	uint8_t sansCouleur = 0b00; // Led eteinte
	uint8_t initCompteur = 0; // initialisation du compteur

   DDRA = 0xff; // PORT A est en mode sortie
   DDRB = 0xff; // PORT B est en mode sortie
   DDRC = 0xff; // PORT C est en mode sortie
   DDRD = 0x00; // PORT D est en mode entree
   //unsigned long compteur=0; // le compteur est initialise a 0.
                             // c'est un compteur de 32 bits
	
	tempsH = tempsTot;	
	//for(uint8_t compteur = 0;compteur <= 3000 ; compteur ++)
	while(tempsH > 0)
	{
	
	tempsH = tempsTot - tempsL;   
	for(int i = 0; i<5 ; i++){
	PORTA = rouge;
	myDelay(tempsH);
	PORTA = sansCouleur;
	myDelay(tempsL);
	}
    tempsL += 1;   
   
   }
   return 0; 
 }
